import 'vue'
import type * as ElementIcons from '@element-plus/icons-vue'

type IconComponents = typeof ElementIcons

declare module 'vue' {
  interface ComponentCustomProperties {
    $tab: typeof import('../plugins/tab')['default']
    $auth: typeof import('../plugins/auth')['default']
    $cache: typeof import('../plugins/cache')['default']
    $modal: typeof import('../plugins/modal')['default']
    $download: typeof import('../plugins/download')['default']
    useDict: typeof import('../utils/dict')['useDict']
    download: typeof import('../utils/request')['download']
    parseTime: typeof import('../utils/ruoyi')['parseTime']
    resetForm: typeof import('../utils/ruoyi')['resetForm']
    handleTree: typeof import('../utils/ruoyi')['handleTree']
    addDateRange: typeof import('../utils/ruoyi')['addDateRange']
    selectDictLabel: typeof import('../utils/ruoyi')['selectDictLabel']
    selectDictLabels: typeof import('../utils/ruoyi')['selectDictLabels']
  }

  interface GlobalComponents extends IconComponents {
    DictTag: typeof import('../components/DictTag/index.vue')['default']
    Pagination: typeof import('../components/Pagination/index.vue')['default']
    TreeSelect: typeof import('../components/TreeSelect/index.vue')['default']
    FileUpload: typeof import('../components/FileUpload/index.vue')['default']
    ImageUpload: typeof import('../components/ImageUpload/index.vue')['default']
    ImagePreview: typeof import('../components/ImagePreview/index.vue')['default']
    RightToolbar: typeof import('../components/RightToolbar/index.vue')['default']
    Editor: typeof import('../components/Editor/index.vue')['default']
    SvgIcon: typeof import('../components/SvgIcon/index.vue')['default']
  }

  interface GlobalDirectives {
    vHasRole: typeof import('../directive/permission/hasRole')['default']
    vHasPermi: typeof import('../directive/permission/hasPermi')['default']
    vCopyText: typeof import('../directive/common/copyText')['default']
  }
}
