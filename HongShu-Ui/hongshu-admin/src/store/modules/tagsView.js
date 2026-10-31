
import { defineStore } from 'pinia'
const useTagsViewStore = defineStore(
  'tags-view',
  {
    state: () => ({
      visitedViews: /** @type {import('@/types/router').TagView[]} */ ([]),
      cachedViews: /** @type {string[]} */ ([]),
      iframeViews: /** @type {import('@/types/router').TagView[]} */ ([])
    }),
    actions: {
      addView(view) {
        this.addVisitedView(view)
        this.addCachedView(view)
      },
      addIframeView(view) {
        if (this.iframeViews.some(v => v.path === view.path)) return
        this.iframeViews.push(
          Object.assign({}, view, {
            title: view.meta.title || 'no-name'
          })
        )
      },
      addVisitedView(view) {
        if (this.visitedViews.some(v => v.path === view.path)) return
        this.visitedViews.push(
          Object.assign({}, view, {
            title: view.meta.title || 'no-name'
          })
        )
      },
      addCachedView(view) {
        if (typeof view.name !== 'string') return
        if (this.cachedViews.includes(view.name)) return
        if (!view.meta.noCache) {
          this.cachedViews.push(view.name)
        }
      },
      async delView(view) {
        await Promise.all([this.delVisitedView(view), this.delCachedView(view)])
        return {
          visitedViews: [...this.visitedViews],
          cachedViews: [...this.cachedViews]
        }
      },
      delVisitedView(view) {
        return new Promise(resolve => {
          for (const [i, v] of this.visitedViews.entries()) {
            if (v.path === view.path) {
              this.visitedViews.splice(i, 1)
              break
            }
          }
          this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
          resolve([...this.visitedViews])
        })
      },
      delIframeView(view) {
        return new Promise(resolve => {
          this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
          resolve([...this.iframeViews])
        })
      },
      delCachedView(view) {
        return new Promise(resolve => {
          const index = typeof view.name === 'string' ? this.cachedViews.indexOf(view.name) : -1
          index > -1 && this.cachedViews.splice(index, 1)
          resolve([...this.cachedViews])
        })
      },
      async delOthersViews(view) {
        await Promise.all([this.delOthersVisitedViews(view), this.delOthersCachedViews(view)])
        return {
          visitedViews: [...this.visitedViews],
          cachedViews: [...this.cachedViews]
        }
      },
      delOthersVisitedViews(view) {
        return new Promise(resolve => {
          this.visitedViews = this.visitedViews.filter(v => {
            return v.meta.affix || v.path === view.path
          })
          this.iframeViews = this.iframeViews.filter(item => item.path === view.path)
          resolve([...this.visitedViews])
        })
      },
      delOthersCachedViews(view) {
        return new Promise(resolve => {
          const index = typeof view.name === 'string' ? this.cachedViews.indexOf(view.name) : -1
          if (index > -1) {
            this.cachedViews = this.cachedViews.slice(index, index + 1)
          } else {
            this.cachedViews = []
          }
          resolve([...this.cachedViews])
        })
      },
      async delAllViews() {
        await Promise.all([this.delAllVisitedViews(), this.delAllCachedViews()])
        return {
          visitedViews: [...this.visitedViews],
          cachedViews: [...this.cachedViews]
        }
      },
      delAllVisitedViews() {
        return new Promise(resolve => {
          this.visitedViews = this.visitedViews.filter(tag => tag.meta.affix)
          this.iframeViews = []
          resolve([...this.visitedViews])
        })
      },
      delAllCachedViews() {
        return new Promise(resolve => {
          this.cachedViews = []
          resolve([...this.cachedViews])
        })
      },
      updateVisitedView(view) {
        for (let v of this.visitedViews) {
          if (v.path === view.path) {
            v = Object.assign(v, view)
            break
          }
        }
      },
      delRightTags(view) {
        return new Promise(resolve => {
          const index = this.visitedViews.findIndex(v => v.path === view.path)
          if (index === -1) {
            resolve([...this.visitedViews])
            return
          }
          this.visitedViews = this.visitedViews.filter((item, idx) => {
            if (idx <= index || (item.meta && item.meta.affix)) {
              return true
            }
            const i = typeof item.name === 'string' ? this.cachedViews.indexOf(item.name) : -1
            if (i > -1) {
              this.cachedViews.splice(i, 1)
            }
            if(item.meta.link) {
              const fi = this.iframeViews.findIndex(v => v.path === item.path)
              if (fi > -1) this.iframeViews.splice(fi, 1)
            }
            return false
          })
          resolve([...this.visitedViews])
        })
      },
      delLeftTags(view) {
        return new Promise(resolve => {
          const index = this.visitedViews.findIndex(v => v.path === view.path)
          if (index === -1) {
            resolve([...this.visitedViews])
            return
          }
          this.visitedViews = this.visitedViews.filter((item, idx) => {
            if (idx >= index || (item.meta && item.meta.affix)) {
              return true
            }
            const i = typeof item.name === 'string' ? this.cachedViews.indexOf(item.name) : -1
            if (i > -1) {
              this.cachedViews.splice(i, 1)
            }
            if(item.meta.link) {
              const fi = this.iframeViews.findIndex(v => v.path === item.path)
              if (fi > -1) this.iframeViews.splice(fi, 1)
            }
            return false
          })
          resolve([...this.visitedViews])
        })
      }
    }
  })

export default useTagsViewStore
