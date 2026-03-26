<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useMousePosition } from '../../composables/useMousePosition'
import Eye from './Eye.vue'
import Pupil from './Pupil.vue'

const props = defineProps<{
  isTyping: boolean
  showPassword: boolean
  passwordLength: number
}>()

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

const backRef = ref<HTMLDivElement | null>(null)
const midRef = ref<HTMLDivElement | null>(null)
const rightRef = ref<HTMLDivElement | null>(null)
const leftRef = ref<HTMLDivElement | null>(null)

const mouse = useMousePosition()

const backBlink = ref(false)
const midBlink = ref(false)
const typingBlink = ref(false)
const peekUp = ref(false)

const timers: number[] = []

function scheduleBlink(target: typeof backBlink) {
  const delay = Math.random() * 4000 + 3000
  const t1 = window.setTimeout(() => {
    target.value = true
    const t2 = window.setTimeout(() => {
      target.value = false
      scheduleBlink(target)
    }, 150)
    timers.push(t2)
  }, delay)
  timers.push(t1)
}

scheduleBlink(backBlink)
scheduleBlink(midBlink)

watch(
  () => props.isTyping,
  (value) => {
    if (!value) {
      typingBlink.value = false
      return
    }

    const timer = window.setTimeout(() => {
      typingBlink.value = true
      const clearTimer = window.setTimeout(() => {
        typingBlink.value = false
      }, 800)
      timers.push(clearTimer)
    }, 800)

    timers.push(timer)
  },
  { immediate: true },
)

watch(
  [() => props.passwordLength, () => props.showPassword, () => peekUp.value],
  ([length, show]) => {
    if (!(length > 0 && show)) {
      peekUp.value = false
      return
    }

    const timer = window.setTimeout(() => {
      peekUp.value = true
      const clearTimer = window.setTimeout(() => {
        peekUp.value = false
      }, 800)
      timers.push(clearTimer)
    }, Math.random() * 3000 + 2000)

    timers.push(timer)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  timers.forEach((timer) => window.clearTimeout(timer))
})

const isCovering = computed(() => props.passwordLength > 0 && !props.showPassword)

function face(refNode: HTMLDivElement | null) {
  if (!refNode) {
    return { faceX: 0, faceY: 0, bodySkew: 0 }
  }

  const rect = refNode.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 3
  const deltaX = mouse.value.x - centerX
  const deltaY = mouse.value.y - centerY

  return {
    faceX: clamp(deltaX / 20, -15, 15),
    faceY: clamp(deltaY / 30, -10, 10),
    bodySkew: clamp(-deltaX / 120, -6, 6),
  }
}

const back = computed(() => face(backRef.value))
const mid = computed(() => face(midRef.value))
const right = computed(() => face(rightRef.value))
const left = computed(() => face(leftRef.value))
</script>

<template>
  <div class="illustration" aria-hidden="true">
    <div
      ref="backRef"
      class="figure figure-back"
      :style="{
        height: isTyping || isCovering ? '440px' : '400px',
        transform: passwordLength > 0 && showPassword
          ? 'skewX(0deg)'
          : isTyping || isCovering
            ? `skewX(${back.bodySkew - 12}deg) translateX(40px)`
            : `skewX(${back.bodySkew}deg)`,
      }"
    >
      <div
        class="face-group face-group-back"
        :style="{
          left: passwordLength > 0 && showPassword ? '20px' : typingBlink ? '55px' : `${45 + back.faceX}px`,
          top: passwordLength > 0 && showPassword ? '35px' : typingBlink ? '65px' : `${40 + back.faceY}px`,
        }"
      >
        <Eye
          :size="18"
          :pupil-size="7"
          :max-distance="5"
          :is-blinking="backBlink"
          :force-look-x="passwordLength > 0 && showPassword ? (peekUp ? 4 : -4) : typingBlink ? 3 : undefined"
          :force-look-y="passwordLength > 0 && showPassword ? (peekUp ? 5 : -4) : typingBlink ? 4 : undefined"
        />
        <Eye
          :size="18"
          :pupil-size="7"
          :max-distance="5"
          :is-blinking="backBlink"
          :force-look-x="passwordLength > 0 && showPassword ? (peekUp ? 4 : -4) : typingBlink ? 3 : undefined"
          :force-look-y="passwordLength > 0 && showPassword ? (peekUp ? 5 : -4) : typingBlink ? 4 : undefined"
        />
      </div>
    </div>

    <div
      ref="midRef"
      class="figure figure-mid"
      :style="{
        transform: passwordLength > 0 && showPassword
          ? 'skewX(0deg)'
          : typingBlink
            ? `skewX(${mid.bodySkew * 1.5 + 10}deg) translateX(20px)`
            : isTyping || isCovering
              ? `skewX(${mid.bodySkew * 1.5}deg)`
              : `skewX(${mid.bodySkew}deg)`,
      }"
    >
      <div
        class="face-group face-group-mid"
        :style="{
          left: passwordLength > 0 && showPassword ? '10px' : typingBlink ? '32px' : `${26 + mid.faceX}px`,
          top: passwordLength > 0 && showPassword ? '28px' : typingBlink ? '12px' : `${32 + mid.faceY}px`,
        }"
      >
        <Eye
          :size="16"
          :pupil-size="6"
          :max-distance="4"
          :is-blinking="midBlink"
          :force-look-x="passwordLength > 0 && showPassword ? -4 : typingBlink ? 0 : undefined"
          :force-look-y="(passwordLength > 0 && showPassword) || typingBlink ? -4 : undefined"
        />
        <Eye
          :size="16"
          :pupil-size="6"
          :max-distance="4"
          :is-blinking="midBlink"
          :force-look-x="passwordLength > 0 && showPassword ? -4 : typingBlink ? 0 : undefined"
          :force-look-y="(passwordLength > 0 && showPassword) || typingBlink ? -4 : undefined"
        />
      </div>
    </div>

    <div
      ref="leftRef"
      class="figure figure-front-left"
      :style="{ transform: passwordLength > 0 && showPassword ? 'skewX(0deg)' : `skewX(${left.bodySkew}deg)` }"
    >
      <div
        class="face-group face-group-front-left"
        :style="{
          left: passwordLength > 0 && showPassword ? '50px' : `${82 + left.faceX}px`,
          top: passwordLength > 0 && showPassword ? '85px' : `${90 + left.faceY}px`,
        }"
      >
        <Pupil
          :size="12"
          :max-distance="5"
          :force-look-x="passwordLength > 0 && showPassword ? -5 : undefined"
          :force-look-y="passwordLength > 0 && showPassword ? -4 : undefined"
        />
        <Pupil
          :size="12"
          :max-distance="5"
          :force-look-x="passwordLength > 0 && showPassword ? -5 : undefined"
          :force-look-y="passwordLength > 0 && showPassword ? -4 : undefined"
        />
      </div>
    </div>

    <div
      ref="rightRef"
      class="figure figure-front-right"
      :style="{ transform: passwordLength > 0 && showPassword ? 'skewX(0deg)' : `skewX(${right.bodySkew}deg)` }"
    >
      <div
        class="face-group face-group-front-right"
        :style="{
          left: passwordLength > 0 && showPassword ? '20px' : `${52 + right.faceX}px`,
          top: passwordLength > 0 && showPassword ? '35px' : `${40 + right.faceY}px`,
        }"
      >
        <Pupil
          :size="12"
          :max-distance="5"
          :force-look-x="passwordLength > 0 && showPassword ? -5 : undefined"
          :force-look-y="passwordLength > 0 && showPassword ? -4 : undefined"
        />
        <Pupil
          :size="12"
          :max-distance="5"
          :force-look-x="passwordLength > 0 && showPassword ? -5 : undefined"
          :force-look-y="passwordLength > 0 && showPassword ? -4 : undefined"
        />
      </div>

      <div
        class="mouth"
        :style="{
          left: passwordLength > 0 && showPassword ? '10px' : `${40 + right.faceX}px`,
          top: passwordLength > 0 && showPassword ? '88px' : `${88 + right.faceY}px`,
        }"
      />
    </div>
  </div>
</template>
