<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { fetchLoginCaptcha, verifyLoginCaptcha } from '../../api/auth'
import { buildCaptchaPointPayload, encryptCaptchaPayload } from '../../utils/captcha-aes'

const props = defineProps<{
  captchaType: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  verified: [captchaVerification: string]
  reset: []
}>()

const DISPLAY_WIDTH = 310
const PUZZLE_Y = 5

const loading = ref(false)
const verifying = ref(false)
const errorMessage = ref('')
const token = ref('')
const secretKey = ref('')
const backgroundBase64 = ref('')
const puzzleBase64 = ref('')
const naturalWidth = ref(DISPLAY_WIDTH)
const sliderOffset = ref(0)
const dragging = ref(false)
const verified = ref(false)

const backgroundStyle = computed(() => (
  backgroundBase64.value ? { backgroundImage: `url(data:image/png;base64,${backgroundBase64.value})` } : undefined
))
const puzzleStyle = computed(() => ({
  backgroundImage: puzzleBase64.value ? `url(data:image/png;base64,${puzzleBase64.value})` : undefined,
  transform: `translateX(${sliderOffset.value}px)`,
}))
const scaleRatio = computed(() => (
  naturalWidth.value > 0 ? naturalWidth.value / DISPLAY_WIDTH : 1
))

const loadCaptcha = async () => {
  loading.value = true
  errorMessage.value = ''
  verified.value = false
  sliderOffset.value = 0
  emit('reset')
  try {
    const challenge = await fetchLoginCaptcha(props.captchaType)
    token.value = challenge.token
    secretKey.value = challenge.secretKey ?? ''
    backgroundBase64.value = challenge.slidingOriginalImageBase64 ?? ''
    puzzleBase64.value = challenge.newSlidingBlockingImageBase64 ?? ''
    await measureBackgroundWidth()
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '验证码加载失败'
  } finally {
    loading.value = false
  }
}

const measureBackgroundWidth = async () => {
  if (!backgroundBase64.value) {
    naturalWidth.value = DISPLAY_WIDTH
    return
  }
  await new Promise<void>((resolve) => {
    const image = new Image()
    image.onload = () => {
      naturalWidth.value = image.naturalWidth || DISPLAY_WIDTH
      resolve()
    }
    image.onerror = () => {
      naturalWidth.value = DISPLAY_WIDTH
      resolve()
    }
    image.src = `data:image/png;base64,${backgroundBase64.value}`
  })
}

const clampOffset = (value: number) => Math.max(0, Math.min(value, DISPLAY_WIDTH - 50))

const updateOffsetFromClientX = (clientX: number, trackLeft: number) => {
  sliderOffset.value = clampOffset(clientX - trackLeft - 25)
}

const onPointerDown = (event: PointerEvent) => {
  if (props.disabled || loading.value || verified.value) {
    return
  }
  dragging.value = true
  const track = (event.currentTarget as HTMLElement).closest('.captcha-slider')
  if (!track) {
    return
  }
  updateOffsetFromClientX(event.clientX, track.getBoundingClientRect().left)
}

const onPointerMove = (event: PointerEvent) => {
  if (!dragging.value) {
    return
  }
  const track = (event.currentTarget as HTMLElement).closest('.captcha-slider')
  if (!track) {
    return
  }
  updateOffsetFromClientX(event.clientX, track.getBoundingClientRect().left)
}

const onPointerUp = async () => {
  if (!dragging.value) {
    return
  }
  dragging.value = false
  await submitCaptcha()
}

const submitCaptcha = async () => {
  if (!token.value || !secretKey.value) {
    return
  }
  verifying.value = true
  errorMessage.value = ''
  try {
    const actualX = Math.round(sliderOffset.value * scaleRatio.value)
    const payload = buildCaptchaPointPayload(secretKey.value, actualX, PUZZLE_Y)
    const answer = await encryptCaptchaPayload(payload, secretKey.value)
    const result = await verifyLoginCaptcha({
      captchaType: props.captchaType,
      token: token.value,
      answer,
    })
    if (!result.captchaVerification) {
      throw new Error('验证码校验未返回凭证')
    }
    verified.value = true
    emit('verified', result.captchaVerification)
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : '验证码校验失败'
    await loadCaptcha()
  } finally {
    verifying.value = false
  }
}

watch(() => props.captchaType, () => {
  void loadCaptcha()
})

onMounted(() => {
  void loadCaptcha()
})
</script>

<template>
  <section class="captcha-panel" :aria-busy="loading || verifying">
    <header class="captcha-panel__header">
      <span>安全验证</span>
      <button type="button" class="captcha-panel__refresh" :disabled="loading || verifying" @click="loadCaptcha">
        刷新
      </button>
    </header>

    <div class="captcha-panel__canvas" :class="{ 'is-verified': verified }">
      <div class="captcha-panel__background" :style="backgroundStyle" />
      <div class="captcha-panel__puzzle" :style="puzzleStyle" />
      <div v-if="loading" class="captcha-panel__mask">加载中...</div>
      <div v-else-if="verified" class="captcha-panel__mask is-success">验证通过</div>
    </div>

    <div
      class="captcha-slider"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointerleave="onPointerUp"
    >
      <div class="captcha-slider__track" />
      <div class="captcha-slider__thumb" :style="{ transform: `translateX(${sliderOffset}px)` }">
        {{ verified ? 'OK' : '>>' }}
      </div>
    </div>

    <p v-if="errorMessage" class="captcha-panel__error" role="alert">{{ errorMessage }}</p>
  </section>
</template>

<style scoped>
.captcha-panel {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.captcha-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--color-text-muted);
  font-size: 12px;
}

.captcha-panel__refresh {
  border: 0;
  background: transparent;
  color: var(--cyan-300);
  cursor: pointer;
}

.captcha-panel__canvas {
  position: relative;
  overflow: hidden;
  width: 310px;
  height: 155px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: rgba(7, 17, 31, 0.65);
}

.captcha-panel__background,
.captcha-panel__puzzle {
  position: absolute;
  inset: 0 auto auto 0;
  width: 310px;
  height: 155px;
  background-repeat: no-repeat;
  background-size: 310px 155px;
}

.captcha-panel__puzzle {
  width: 50px;
  height: 155px;
  background-size: 50px 155px;
  pointer-events: none;
}

.captcha-panel__mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(7, 17, 31, 0.55);
  color: var(--color-text-muted);
  font-size: 13px;
}

.captcha-panel__mask.is-success {
  color: var(--cyan-300);
}

.captcha-slider {
  position: relative;
  width: 310px;
  height: 40px;
  touch-action: none;
  user-select: none;
}

.captcha-slider__track {
  position: absolute;
  inset: 12px 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.captcha-slider__thumb {
  position: absolute;
  top: 0;
  left: 0;
  display: grid;
  width: 50px;
  height: 40px;
  place-items: center;
  border: 1px solid var(--color-border-strong);
  border-radius: 999px;
  background: rgba(33, 200, 239, 0.15);
  color: var(--cyan-300);
  cursor: grab;
}

.captcha-panel__error {
  margin: 0;
  color: #ff8f8f;
  font-size: 12px;
}
</style>
