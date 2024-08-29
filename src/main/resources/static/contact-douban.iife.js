(function () {
    function getDoubanDetail(src, e) {
        var url = '/apis/api.douban.moony.la/v1alpha1/doubanmovies/-/getDoubanDetail?url=' + src;
        fetch(url)
                .then(response => response.json())
                .then(data => {
                    renderer(data, e);
                })
                .catch(console.error)
    }

    function renderer(data, e) {
        let img = data.spec.dataType == 'db' ? `${data.spec.poster}` : data.spec.poster
        let date = new Date(data.faves.createTime);
        let dateString = date.toLocaleString(); // 使用默认的日期和时间格式
        const r = document.createElement("div");
        r.classList.add('doulist-item')
        r.innerHTML = `<div class="doulist-subject">
            <div class="doulist-post"><img decoding="async" referrerpolicy="no-referrer" class="fade-before fade-after"
                    src="${img}"></div>
            <div class="db--viewTime JiEun">Marked ${dateString}</div>
            <div class="doulist-content">
                <div class="doulist-title"><a class="cute" target="_blank" rel="external nofollow"
                        href="${data.spec.link}">${data.spec.name}</a></div>
                <div class="rating"><span class="allstardark"><span class="allstarlight" style="width: ${data.spec.score * 10}%;"></span></span><span
                        class="rating_nums">${data.spec.score}</span></div>
                <div class="abstract">${data.faves?.remark != null && data.faves?.remark != '' ? data.faves?.remark : data.spec.cardSubtitle}</div>
            </div>
        </div>`
        e.appendChild(r);
    }

    const mf = () => {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    const e = entry.target;
                    const src = e.getAttribute("src");
                    if (src != null && src != '') {
                        getDoubanDetail(src, e);
                        observer.unobserve(e); // 停止观察已经加载的元素，防止重复加载
                    }
                }
            });
        });

        document.querySelectorAll("douban").forEach((e) => {
            observer.observe(e);
        });
    };

    document.addEventListener("DOMContentLoaded", () => {
        mf();
    }, {
        once: true
    });

    document.addEventListener("pjax:success", () => {
        mf();
    });

})();
