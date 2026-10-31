import { formatDictLabels } from './dictLabels'

/**
 * 通用js方法封装处理
 */

export {
  parseTime,
  resetForm,
  selectDictLabel,
  sprintf,
  parseStrEmpty,
  mergeRecursive,
  handleTree,
  tansParams,
} from './ruoyi'

// 添加日期范围
export function addDateRange(params, dateRange, propName) {
  let search = params;
  search.params = typeof (search.params) === 'object' && search.params !== null && !Array.isArray(search.params) ? search.params : {};
  dateRange = Array.isArray(dateRange) ? dateRange : [];
  if (typeof (propName) === 'undefined') {
    search['startDate'] = dateRange[0];
    search['endDate'] = dateRange[1];
  } else {
    search[propName + 'startDate'] = dateRange[0];
    search[propName + 'endDate'] = dateRange[1];
  }
  return search;
}

// 回显数据字典（字符串数组）
export function selectDictLabels(datas, value, separator) {
  if (value === undefined) {
    return "";
  }
  return formatDictLabels(datas, value, separator);
}

// 验证是否为blob格式
export async function blobValidate(data) {
  try {
    const text = await data.text();
    JSON.parse(text);
    return false;
  } catch (error) {
    return true;
  }
}
