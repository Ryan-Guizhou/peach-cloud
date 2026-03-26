<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMousePosition } from '../../composables/useMousePosition'

const props = withDefaults(
  defineProps<{
    size?: number
    maxDistance?: number
    pupilColor?: string
    forceLookX?: number
    forceLookY?: number
  }>(),
  {
    size: 12,
    maxDistance: 5,
    pupilColor: '#172033',
  },
)

const node = ref<HTMLDivElement | null>(null)
const mouse = useMousePosition()

const styleObject = computed(() => {
  if (!node.value) {
    return {
      width: `${props.size}px`,
      height: `${props.size}px`,
      backgroundColor: props.pupilColor,
      transform: 'translate(0px, 0px)',
    }
  }

  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return {
      width: `${props.size}px`,
      height: `${props.size}px`,
      backgroundColor: props.pupilColor,
      transform: `translate(${props.forceLookX}px, ${props.forceLookY}px)`,
    }
  }

  const rect = node.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 2
  const deltaX = mouse.value.x - centerX
  const deltaY = mouse.value.y - centerY
  const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), props.maxDistance)
  const angle = Math.atan2(deltaY, deltaX)
  const x = Math.cos(angle) * distance
  const y = Math.sin(angle) * distance

  return {
    width: `${props.size}px`,
    height: `${props.size}px`,
    backgroundColor: props.pupilColor,
    transform: `translate(${x}px, ${y}px)`,
  }
})
</script>

<template>
  <div ref="node" class="pupil" :style="styleObject" />
</template>
