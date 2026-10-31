
import { defineStore } from 'pinia'
import Cookies from 'js-cookie'

const useAppStore = defineStore(
  'app',
  {
    state: () => ({
      sidebar: {
        opened: Cookies.get('sidebarStatus') ? !!+Cookies.get('sidebarStatus') : true,
        hovered: false,
        withoutAnimation: false,
        hide: false
      },
      device: 'desktop'
    }),
    actions: {
      toggleSideBar(withoutAnimation = false) {
        if (this.sidebar.hide) {
          return false;
        }
        this.sidebar.opened = !this.sidebar.opened
        this.sidebar.hovered = false
        this.sidebar.withoutAnimation = withoutAnimation
        if (this.sidebar.opened) {
          Cookies.set('sidebarStatus', 1)
        } else {
          Cookies.set('sidebarStatus', 0)
        }
      },
      closeSideBar({ withoutAnimation }) {
        Cookies.set('sidebarStatus', 0)
        this.sidebar.opened = false
        this.sidebar.hovered = false
        this.sidebar.withoutAnimation = withoutAnimation
      },
      toggleDevice(device) {
        this.device = device
        this.sidebar.hovered = false
      },
      setSidebarHovered(hovered) {
        this.sidebar.hovered = Boolean(hovered && this.device === 'desktop' && !this.sidebar.opened && !this.sidebar.hide)
        if (this.sidebar.hovered) this.sidebar.withoutAnimation = false
      },
      toggleSideBarHide(status) {
        this.sidebar.hide = status
        if (status) this.sidebar.hovered = false
      }
    }
  })

export default useAppStore
