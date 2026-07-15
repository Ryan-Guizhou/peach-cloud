<script setup lang="ts">
import Pupil from './Pupil.vue'

withDefaults(
  defineProps<{
    size?: number
    pupilSize?: number
    maxDistance?: number
    eyeColor?: string
    pupilColor?: string
    isBlinking?: boolean
    forceLookX?: number
    forceLookY?: number
  }>(),
  {
    size: 48,
    pupilSize: 16,
    maxDistance: 10,
    eyeColor: '#ffffff',
    pupilColor: '#172033',
    isBlinking: false,
  },
)
</script>

<template>
  <div
    class="eye-ball"
    :style="{
      width: `${size}px`,
      height: isBlinking ? '2px' : `${size}px`,
      backgroundColor: eyeColor,
    }"
  >
    <Pupil
      v-if="!isBlinking"
      :size="pupilSize"
      :max-distance="maxDistance"
      :pupil-color="pupilColor"
      :force-look-x="forceLookX"
      :force-look-y="forceLookY"
    />
  </div>
</template>
