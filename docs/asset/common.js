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

function highlightCode() {
    if (!window.hljs)
        return;

    document.querySelectorAll('pre code').forEach(function (codeElement) {
        // Preserve the unhighlighted source for the copy button before Highlight.js changes the markup.
        codeElement.parentNode.dataset.code = codeElement.textContent;
        if (!codeElement.dataset.highlighted)
            window.hljs.highlightElement(codeElement);
    });

}

// Highlight.js is loaded with defer and is available before DOMContentLoaded.
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
