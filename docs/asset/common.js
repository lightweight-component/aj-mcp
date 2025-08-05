/* 百度统计 */
var _hmt = _hmt || [];
(function() {
  var hm = document.createElement("script");
  hm.src = "https://hm.baidu.com/hm.js?15cec415fe8edf62b6d630ea60ca2b07";
  var s = document.getElementsByTagName("script")[0];
  s.parentNode.insertBefore(hm, s);
})();

document.addEventListener('DOMContentLoaded', function () {
    var arr = location.pathname.split('/');

    if (arr.length == 3)
        arr.unshift('');

    if (arr && arr.length > 3) {
        arr.pop();

        let m = arr.pop();

        if (m === 'cn')
            m = arr.pop();

        if (m) {
            const selected = document.querySelector('body>div>menu li.selected');
            selected && selected.classList.remove('selected');

            document.querySelectorAll('body>div>menu li a').forEach(a => {
                console.log(a.getAttribute('href'))
                if (a.getAttribute('href').indexOf(m) != -1) {
                    console.log(m)
                    a.parentNode.classList.add('selected');
                }
            });
        }
    }

    document.querySelectorAll('pre').forEach(pre => {
        var div = document.createElement('div');
        div.innerHTML = '📋';
        var codeEl = pre.querySelector('code');
        pre.insertBefore(div, codeEl);
        div.onclick = function (e) {
            let code = pre.dataset.code;

            if (code) {
                code = code.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>');

                navigator.clipboard.writeText(code).then(() => {
                    console.log('文本已成功复制到剪贴板');
                    div.innerHTML = '📋Copied.'
                }).catch(err => {
                    console.error('无法复制文本: ', err);
                    fallbackCopyTextToClipboard(text); // 回退方案
                });
            }
        }
    });
});

const tagRegex = /&lt;([^&]+)&gt;/g;
const attributeRegex = /(\w+)\s*=\s*["']([^"']*)["']/g;
const getValue = /\="([^"]*)"/;

function matchAttrib(_m) {
    _m = _m.replace(getValue, '=<span class="xml-string">"$1"</span>');

    return '<span class="xml-attribute">' + _m + '</span>';
}

function match1(match, b, c) {
    if (match.indexOf('&lt;!') != -1)
        return '<span class="xml-comment">' + match + '</span>';
    else {
        match = match.replace(attributeRegex, matchAttrib);

        return '<span class="xml-tag">' + match + '</span>';
    }
}

// SQL DDL 关键字
const ddlKeywords = [
    "CREATE",
    "TABLE",
    "PRIMARY",
    "KEY",
    "FOREIGN",
    "CONSTRAINT",
    "NOT",
    "NULL",
    "UNIQUE"
];

// SQL 关键字
const sqlKeywords = [
    "SELECT",
    "FROM",
    "WHERE",
    "LIKE",
    "BETWEEN",
    "NOT",
    "FALSE",
    "NULL",
    "TRUE",
    "IN",
    "INSERT",
    "INTO",
    "VALUES"
];

// 高亮 SQL DDL 关键字
function highlightDDLKeywords(text, isSqlDDL) {
    var words = text.split(' ');
    var highlightedText = '';

    words.forEach(function (word) {
        if ((isSqlDDL ? ddlKeywords : sqlKeywords).includes(word.trim().toUpperCase()))
            highlightedText += '<span class="sql-ddl-keyword">' + word + '</span> ';
        else
            highlightedText += word + ' ';
    });

    return highlightedText;
}


// Java 关键字
const javaKeywordsRegex = /\b(new|return|public|static|class|void|int|boolean|if|else|while|for|switch|case|break|default|try|catch|finally|throw|throws|private|protected|package|import|interface|implements|extends|this|super|instanceof|final|abstract|synchronized|volatile)\b/g;
const matchJavaAnn = /@\w+/;

const matchString = /'[^']*'/g;
const matchString2 = /"[^"]*"/g;
const matchSqlField = /`[^`]*`/g;

// 匹配 YAML 键名
const keyRegex = /^(\s*)(\w+)(\s*):/gm;
// 匹配 YAML 字符串值
const valueRegex = /(['"])(.*?)\1/g;

function highlightCode() {
    var codeElement = document.querySelectorAll('.language-xml');

    codeElement && codeElement.length && codeElement.forEach(item => {
        var xmlCode = item.innerHTML;
        item.parentNode.dataset.code = xmlCode;
        // 替换特殊字符
        xmlCode = xmlCode/* .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') */.replace(tagRegex, match1);

        // 将处理后的内容重新放回 <code> 元素中
        item.innerHTML = xmlCode;
    });

    codeElement = document.querySelectorAll('.sql-ddl-code');

    codeElement && codeElement.length && codeElement.forEach(item => {
        let code = item.innerHTML;
        item.parentNode.dataset.code = code;
        code = code.replace(matchString, m => `<span class="code-string">${m}</span>`);
        code = code.replace(matchSqlField, m => `<span class="sql-field">${m}</span>`);

        item.innerHTML = highlightDDLKeywords(code, true);
    });

    codeElement = document.querySelectorAll('.sql-code');

    codeElement && codeElement.length && codeElement.forEach(item => {
        let code = item.innerHTML;
        item.parentNode.dataset.code = code;
        code = code.replace(matchString, m => `<span class="code-string">${m}</span>`);
        code = code.replace(matchSqlField, m => `<span class="sql-field">${m}</span>`);

        item.innerHTML = highlightDDLKeywords(code);
    });

    codeElement = document.querySelectorAll('.language-java');

    codeElement && codeElement.length && codeElement.forEach(item => {
        let code = item.innerHTML;
        item.parentNode.dataset.code = code;
        code = code.replace(matchString2, m => `<span Class="code-string">${m}</span>`);
        code = code.replace(javaKeywordsRegex, '<span class="java-keyword">$1</span>');
        code = code.replace(matchJavaAnn, m => `<span class="java-anno">${m}</span>`);

        item.innerHTML = code;
    });

    codeElement = document.querySelectorAll('.yaml-code');

    codeElement && codeElement.length && codeElement.forEach(item => {
        let code = item.innerHTML;
        item.parentNode.dataset.code = code;
        const yamlKeywords = ["true", "false", "null"];

        // YAML语法规则的正则表达式
        const regex = /(:|\s+|-|#.*$|".*"|'.*'|\d+|\btrue\b|\bfalse\b|\bnull\b)/g;

        // 将文本中的关键字和字符串用<span>标签包裹起来
        code = code.replace(regex, function (match) {
            if (yamlKeywords.includes(match))
                return '<span class="yaml-keyword">' + match + '</span>';
            else if (match.startsWith('#'))
                return '<span class="yaml-comment">' + match + '</span>';
            else if (match.startsWith('"') || match.startsWith("'"))
                return '<span class="yaml-string">' + match + '</span>';
            else
                return match;
        });

        item.innerHTML = code;
    });
}

// 页面加载完成后，调用 highlightCode 函数
document.addEventListener('DOMContentLoaded', highlightCode);

// 获取用户的默认语言
var userLang = navigator.language || navigator.userLanguage;

// 检查是否为中文环境（包括简体和繁体）
if (userLang.startsWith('zh') && location.pathname.indexOf('cn') == -1) {
     confirm('欢迎！您可以改为访问中文内容。是否继续？') && location.assign('/cn');  // 如果是中文，则弹出提示
}

function loadScript(url, callback) {
    // 创建一个 <script> 元素
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = url;

    // 判断是否支持 onload 事件
    if (script.readyState) { // For IE6-10
        script.onreadystatechange = function () {
            if (script.readyState === 'loaded' || script.readyState === 'complete') {
                script.onreadystatechange = null; // 防止多次执行
                if (callback) callback();
            }
        };
    } else { // For modern browsers
        script.onload = function () {
            if (callback) callback();
        };
    }

    // 将 <script> 元素添加到 <head> 或 <body> 中
    document.getElementsByTagName('head')[0].appendChild(script);
}

function xhrGet(url, callback) {
    // 创建一个 XMLHttpRequest 对象
    const xhr = new XMLHttpRequest();

    // 配置请求类型和 URL
    xhr.open('GET', url, true);

    // 设置请求头（如果需要）
    xhr.setRequestHeader('Content-Type', 'application/json');

    // 注册事件监听器
    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) { // 请求已完成
        if (xhr.status === 200) {
          // 请求成功，处理响应数据
          let resp = JSON.parse(xhr.responseText);
          console.log('响应数据:', resp);
          callback(resp);
        } else {
          // 请求失败，处理错误
          console.error('请求失败，状态码：', xhr.status);
        }
      }
    };
    // 发送请求
    xhr.send();
}

loadScript('https://cdn.jsdelivr.net/npm/vue@2.6.14/dist/vue.min.js', function () {
    new Vue({
        el: '#comment-holder',
        data: {
            showInput: true,
            logined: false,
            listData:[]
        },
        methods: {
             loginWeibo() {
                let clientId = '360568732';
                let redirectUri = 'http://localhost:8081/oauth_callback/callback/weibo.action';
                let url = 'https://api.weibo.com/oauth2/authorize?client_id=' + clientId;

                url += '&response_type=code&state=register&redirect_uri=';
                url += redirectUri;

                console.log(url);
                let weiboAuthWin = window.open(url, '微博授权登录', 'width=770,height=600,menubar=0,scrollbars=1,resizable=1,status=1,titlebar=0,toolbar=0,location=1');
                console.log(weiboAuthWin);
                weiboAuthWin.document.title = '微博授权登录';

                // 监听子窗口的消息
                window.addEventListener("message", (event) => {
                    // 确保消息来源是可信的（可以通过 event.origin 验证）
                    console.log("Message from child:", event.data);
                });
            }
        },
        mounted() {
         xhrGet('http://localhost:8081/comment?url=sds', (json) => {
//            debugger;
            let list = json.data.list;
            this.listData = list;
         });
        },

        template: `<div id="comment-holder">
            <div :class="{'input-box': true, showInput: showInput}">
                <textarea placeholder="参与评论" @click="showInput = true"></textarea>
                <div class="btn">
                    <div class="login">
                        评论前先登录： <a href="https://github.com/login/oauth/authorize?client_id=Iv1.b0c7f0c7f0c7f0c7&scope=user">GitHub</a> | <a href="javascript:void(0);" @click="loginWeibo">微博</a>
                    </div>
                    <button @click="showInput = false">取 消</button>
                    <button>提 交</button>
                </div>
            </div>
            <ul>
            <li v-for="(item, index) in listData" :key="index"><div>
            <div class="avatar">
                <img :src="'data:image/jpg;base64,' + item.avatar" />
            </div>
            <div class="right">
                <div>
                    <span class="user-name">Jack</span>
                    <span class="time">• 3小时前 </span>
                </div>
                <div class="content">{{item.content}}</div>
                <div class="toolbar"><a href="#">回复</a><a href="#">编辑</a><a href="#">删除</a></div>
            </div>
        </div></li></ul></div>`
    });
});