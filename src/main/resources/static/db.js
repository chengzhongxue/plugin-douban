// @ts-nocheck
class HALO_DOUBAN {
    constructor() {
        this.ver = "1.0.0";
        this.type = "movie";
        this.status = "done";
        this.finished = false;
        this.paged = 1;
        this.genre_list = [];
        this.genre = null;
        this.subjects = [];
        this._create();
    }

    on(t, e, n) {
        var a = document.querySelectorAll(e);
        a.forEach((item) => {
            item.addEventListener(t, n);
        });
    }
    _fetchGenres() {
        document.querySelector(".db--genres").innerHTML = "";
        const url = '/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-douban/genres';
        fetch(url)
                .then((response) => response.json())
                .then((data) => {
                    if(data.length){
                        this.genre_list = data;
                        this._renderGenre();
                    }
                });
        return true;
    }

    _statusChange() {
        this.on("click", ".db--typeItem", (t) => {
            const self = t.currentTarget;
            if (self.classList.contains("is-active")) {
                // const index = this.genre.indexOf(self.innerText);
                return;
            }
            document.querySelector(".db--list").innerHTML = "";
            document.querySelector(".lds-ripple").classList.remove("u-hide");
            document
                    .querySelector(".db--typeItem.is-active")
                    .classList.remove("is-active");
            self.classList.add("is-active");
            this.status = self.dataset.status;
            this.paged = 1;
            this.finished = false;
            this.subjects = [];
            this._fetchData();
            return;
        });
    }

    _handleGenreClick() {
        this.on("click", ".db--genreItem", (t) => {
            var genreItem =  document.querySelector(".db--genreItem.is-active")
            if(genreItem!=null){
                genreItem.classList.remove("is-active");
            }
            const self = t.currentTarget;
            document.querySelector(".db--list").innerHTML = "";
            document.querySelector(".lds-ripple").classList.remove("u-hide");

            self.classList.add("is-active");
            if(self.innerText=='全部'){
                this.genre = null;
            }else{
                this.genre = self.innerText;
            }
            this.paged = 1;
            this.finished = false;
            this.subjects = [];
            this._fetchData();
            return;
        });
    }

    _renderGenre() {
        var index = 0;
        document.querySelector(".db--genres").innerHTML = this.genre_list
                .map((item) => {
                    index = index+1;
                    if(index==1){
                        return `<span class="db--genreItem is-active">全部</span><span class="db--genreItem">${item}</span>`;
                    }
                    return `<span class="db--genreItem">${item}</span>`;
                })
                .join("");
        this._handleGenreClick();
    }

    _fetchData() {
        var url = `/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-douban/doubanmovies?page=${this.paged}&size=49&type=${this.type}&status=${this.status}`;
        if(this.genre!=null && this.genre!=''){
            url = url + `&genre=${this.genre}`
        }
        fetch(url)
                .then((response) => response.json())
                .then((data) => {
                    if (data.items.length) {
                        if(this.subjects==null){
                            this.subjects =[]
                        }
                        if (
                                document
                                        .querySelector(".db--list")
                                        .classList.contains("db--list__card")
                        ) {
                            this.subjects = [...this.subjects, ...data.items];
                            this._randerDateTemplate();
                        } else {
                            this.subjects = [...this.subjects, ...data.items];
                            this._randerListTemplate();
                        }
                        document
                                .querySelector(".lds-ripple")
                                .classList.add("u-hide");
                    } else {
                        document
                                .querySelector(".db--list")
                                .classList.contains("db--list__card")
                                ? this._randerDateTemplate()
                                : this._randerListTemplate();
                        this.finished = true;
                        document
                                .querySelector(".lds-ripple")
                                .classList.add("u-hide");
                    }
                });
    }

    _randerDateTemplate() {
        if (!this.subjects.length)
            return (document.querySelector(
                    ".db--list"
            ).innerHTML = `<div class="db--empty"></div>`);
        const result = this.subjects.reduce((result, item) => {
            const date = new Date(item.faves.createTime);
            const year = date.getFullYear();
            const month = date.getMonth() + 1;
            const key = `${year}-${month.toString().padStart(2, "0")}`;
            if (Object.prototype.hasOwnProperty.call(result, key)) {
                result[key].push(item);
            } else {
                result[key] = [item];
            }
            return result;
        }, {});
        let html = ``;
        for (let key in result) {
            const date = key.split("-");
            html += `<div class="db--listBydate"><div class="db--titleDate JiEun"><div class="db--titleDate__day">${date[1]}</div><div class="db--titleDate__month">${date[0]}</div></div><div class="db--dateList__card">`;
            html += result[key]
                    .map((movie) => {
                        var img = `https://dou.img.lithub.cc/${movie.spec.type}/${movie.spec.id}.jpg`
                        return `<div class="db--item">""<img src="${movie.spec.dataType == 'halo'? movie.spec.poster : img}" referrerpolicy="unsafe-url" class="db--image">
                         <div class="db--score JiEun">${
                                movie.spec.score > 0
                                        ? '<svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" ><path d="M12 20.1l5.82 3.682c1.066.675 2.37-.322 2.09-1.584l-1.543-6.926 5.146-4.667c.94-.85.435-2.465-.799-2.567l-6.773-.602L13.29.89a1.38 1.38 0 0 0-2.581 0l-2.65 6.53-6.774.602C.052 8.126-.453 9.74.486 10.59l5.147 4.666-1.542 6.926c-.28 1.262 1.023 2.26 2.09 1.585L12 20.099z"></path></svg>' +
                                        movie.spec.score
                                        : ""
                        }${
                                movie.spec.year > 0 ? " · " + movie.spec.year : ""
                        }</div><div class="db--title"><a href="${
                                movie.spec.link
                        }" target="_blank">${movie.spec.name}</a></div>
    
    </div>`;
                    })
                    .join("");
            html += `</div></div>`;
        }
        document.querySelector(".db--list").innerHTML = html;
    }

    _randerListTemplate() {
        if (!this.subjects.length)
            return (document.querySelector(
                    ".db--list"
            ).innerHTML = `<div class="db--empty"></div>`);
        document.querySelector(".db--list").innerHTML = this.subjects
                .map((item) => {
                    var img = `https://dou.img.lithub.cc/${item.spec.type}/${item.spec.id}.jpg`
                    return `<div class="db--item">""<img src="${item.spec.dataType == 'halo'? item.spec.poster : img}" referrerpolicy="unsafe-url" class="db--image">
                              <div class="ipc-signpost JiEun">${item.faves.createTime}</div>
                             <div class="db--score JiEun">${
                            item.spec.score > 0
                                    ? '<svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" ><path d="M12 20.1l5.82 3.682c1.066.675 2.37-.322 2.09-1.584l-1.543-6.926 5.146-4.667c.94-.85.435-2.465-.799-2.567l-6.773-.602L13.29.89a1.38 1.38 0 0 0-2.581 0l-2.65 6.53-6.774.602C.052 8.126-.453 9.74.486 10.59l5.147 4.666-1.542 6.926c-.28 1.262 1.023 2.26 2.09 1.585L12 20.099z"></path></svg>' +
                                    item.spec.score
                                    : ""
                    }${
                            item.spec.year > 0 ? " · " + item.spec.year : ""
                    }</div><div class="db--title"><a href="${
                            item.spec.link
                    }" target="_blank">${item.spec.name}</a></div>
                </div>
                </div>`;
                })
                .join("");
    }

    _handleScroll() {
        window.addEventListener("scroll", () => {
            var t = window.scrollY || window.pageYOffset;
            // @ts-ignore
            if (
                    document.querySelector(".block-more").offsetTop +
                    // @ts-ignore
                    -window.innerHeight <
                    t &&
                    document
                            .querySelector(".lds-ripple")
                            .classList.contains("u-hide") &&
                    !this.finished
            ) {
                document
                        .querySelector(".lds-ripple")
                        .classList.remove("u-hide");
                this.paged++;
                this._fetchData();
            }
        });
    }

    _handleNavClick() {
        this.on("click", ".db--navItem", (t) => {
            if (t.target.classList.contains("current")) return;
            this.genre = null;
            this.type = t.target.dataset.type;
            if (this.type != "book") {
                this._fetchGenres();
                document
                        .querySelector(".db--genres")
                        .classList.remove("u-hide");
            } else {
                document.querySelector(".db--genres").classList.add("u-hide");
            }
            document.querySelector(".db--list").innerHTML = "";
            document.querySelector(".lds-ripple").classList.remove("u-hide");
            document
                    .querySelector(".db--navItem.current")
                    .classList.remove("current");
            const self = t.target;
            self.classList.add("current");
            this.paged = 1;
            this.finished = false;
            this.subjects = null;
            this._fetchData();
        });
    }

    _create() {
        if (document.querySelector(".db--container")) {
            if (document.querySelector(".db--navItem.current")) {
                this.type = document.querySelector(
                        ".db--navItem.current"
                ).dataset.type;
            }
            if (document.querySelector(".db--list").dataset.type)
                this.type = document.querySelector(".db--list").dataset.type;
            if (this.type == "movie") {
                document
                        .querySelector(".db--genres")
                        .classList.remove("u-hide");
            }
            this._fetchGenres();
            this._fetchData();
            this._handleScroll();
            this._handleNavClick();
            this._statusChange();
        }
    }
}

new HALO_DOUBAN();
