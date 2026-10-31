// 两种工具入口共用字典标签格式化，各入口保留自己的空值和数组处理约定。
export function formatDictLabels(datas, value, separator) {
  const actions = [];
  const currentSeparator = undefined === separator ? "," : separator;
  const temp = value.split(currentSeparator);
  Object.keys(value.split(currentSeparator)).some((val) => {
    let match = false;
    Object.keys(datas).some((key) => {
      if (String(datas[key].value) === ('' + temp[val])) {
        actions.push(datas[key].label + currentSeparator);
        match = true;
      }
    })
    if (!match) {
      actions.push(temp[val] + currentSeparator);
    }
  })
  return actions.join('').substring(0, actions.join('').length - 1);
}
