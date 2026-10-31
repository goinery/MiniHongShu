import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { blobValidate } from '@/utils/ruoyi'

const baseURL = import.meta.env.VITE_APP_BASE_API

async function downloadFile(url) {
  const res = await axios({
    method: 'get',
    url,
    responseType: 'blob',
    headers: { 'Authorization': 'Bearer ' + getToken() }
  })
  if (blobValidate(res.data)) {
    saveAs(new Blob([res.data]), decodeURIComponent(res.headers['download-filename']))
  } else {
    await printErrMsg(res.data)
  }
}

async function printErrMsg(data) {
  const resText = await data.text();
  const rspObj = JSON.parse(resText);
  const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
  ElMessage.error(errMsg);
}

export default {
  name(name, isDelete = true) {
    const url = baseURL + "/common/download?fileName=" + encodeURIComponent(name) + "&delete=" + isDelete
    return downloadFile(url)
  },
  resource(resource) {
    const url = baseURL + "/common/download/resource?resource=" + encodeURIComponent(resource);
    return downloadFile(url)
  },
  zip(url, name) {
    const downloadUrl = baseURL + url
    const downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
    return axios({
      method: 'get',
      url: downloadUrl,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then(async (res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        await this.printErrMsg(res.data);
      }
    }).catch((r) => {
      console.error(r)
      ElMessage.error('下载文件出现错误，请联系管理员！')
    }).finally(() => {
      downloadLoadingInstance.close();
    })
  },
  saveAs(text, name, opts) {
    saveAs(text, name, opts);
  },
  printErrMsg
}

