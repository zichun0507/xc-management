import type { App } from 'vue'
import { useUserStore } from '@/stores/user'

export function setupPermissionDirective(app: App) {
  app.directive('permission', {
    mounted(el: HTMLElement, binding) {
      const userStore = useUserStore()
      const required = binding.value
      if (typeof required === 'string') {
        if (userStore.role !== required) {
          el.style.display = 'none'
        }
      } else if (Array.isArray(required)) {
        if (!required.includes(userStore.role)) {
          el.style.display = 'none'
        }
      }
    },
  })
}