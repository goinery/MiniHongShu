import 'vue-router'
import type { LocationQuery, RouteMeta, RouteParams, RouteRecordName } from 'vue-router'

export interface TagView {
  path: string
  fullPath: string
  name?: RouteRecordName | null
  meta: RouteMeta
  query?: LocationQuery
  params?: RouteParams
  title?: string
}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    noCache?: boolean
    breadcrumb?: boolean
    activeMenu?: string
    affix?: boolean
    link?: string
  }
}
