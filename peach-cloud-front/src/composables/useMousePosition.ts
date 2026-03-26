import { onMounted, onUnmounted, ref } from 'vue'

const mouse = ref({ x: 0, y: 0 })
let bindCount = 0
let handler: ((event: MouseEvent) => void) | null = null

export function useMousePosition() {
  onMounted(() => {
    bindCount += 1

    if (!handler) {
      handler = (event: MouseEvent) => {
        mouse.value = { x: event.clientX, y: event.clientY }
      }
      window.addEventListener('mousemove', handler)
    }
  })

  onUnmounted(() => {
    bindCount -= 1

    if (bindCount <= 0 && handler) {
      window.removeEventListener('mousemove', handler)
      handler = null
      bindCount = 0
    }
  })

  return mouse
}
