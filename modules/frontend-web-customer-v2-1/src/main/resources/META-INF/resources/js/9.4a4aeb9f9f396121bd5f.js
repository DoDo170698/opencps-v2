webpackJsonp([9,16],{236:function(t,a,e){var s=e(146)(e(577),e(581),null,null);t.exports=s.exports},259:function(t,a,e){var s=e(146)(e(609),e(616),null,null);t.exports=s.exports},577:function(t,a,e){"use strict";Object.defineProperty(a,"__esModule",{value:!0});a.default={name:"TinyPagination",props:{total:{type:Number,required:!0},page:{type:Number,default:1},lang:{type:String,default:"en"},customClass:{type:String},limits:{type:Array,default:function(){return[10,15,20,50,100]}},showLimit:{type:Boolean,default:!0}},data:function(){return{version:"0.2.1",currentPage:1,currentLimit:15,translations:{en:{prev:"Previous",title:"Page",next:"Next"}},availableLanguages:["en"]}},created:function(){this.currentPage=this.page},watch:{page:function(t){this.currentPage=parseInt(t)}},computed:{translation:function(){return this.availableLanguages.includes(this.lang)?this.translations[this.lang]:this.translations.en},totalPages:function(){return Math.ceil(this.total/this.currentLimit)},totalPagesData:function(){for(var t=[],a={},e=1;e<=this.totalPages;++e)a={value:e,text:"Trang "+e},t.push(a);return t},titlePage:function(){return""+this.currentPage},classFirstPage:function(){return{"c-not-allowed pagination__navigation--disabled":1===this.currentPage}},classLastPage:function(){return{"c-not-allowed pagination__navigation--disabled":this.currentPage===this.totalPages}}},methods:{nextPage:function(){this.currentPage!==this.totalPages&&(this.currentPage+=1,this.$emit("tiny:change-page",{page:this.currentPage}))},lastPage:function(){this.currentPage>1&&(this.currentPage-=1,this.$emit("tiny:change-page",{page:this.currentPage}))},nextPageLast:function(){this.currentPage=this.totalPages,this.$emit("tiny:change-page",{page:this.totalPages})},lastPageLast:function(){this.currentPage=1,this.$emit("tiny:change-page",{page:1})},goToPage:function(){this.$emit("tiny:change-page",{page:this.currentPage})},onLimitChange:function(){this.currentPage=1}}}},581:function(t,a){t.exports={render:function(){var t=this,a=t.$createElement,e=t._self._c||a;return e("div",[e("div",{staticStyle:{"text-align":"left",position:"absolute","line-height":"46px"}},[t._v("Tổng số "),e("span",{staticClass:"text-bold primary--text"},[t._v(t._s(t.total))]),t._v(" bản ghi. ")]),t._v(" "),t.total>0?e("div",{staticClass:"vue-tiny-pagination pagination layout",staticStyle:{"justify-content":"flex-end","-webkit-justify-content":"flex-end"}},[e("div",{staticClass:"px-3 xs4 flex"},[e("v-select",{attrs:{items:t.totalPagesData,"item-text":"text","item-value":"value",autocomplete:""},on:{input:t.goToPage},model:{value:t.currentPage,callback:function(a){t.currentPage=a},expression:"currentPage"}})],1),t._v(" "),e("ul",{staticClass:"tiny-pagination",class:t.customClass},[e("li",{staticClass:"page-item",class:t.classFirstPage},[e("button",{staticClass:"pagination__navigation",class:t.classFirstPage,on:{click:function(a){return a.preventDefault(),t.lastPageLast(a)}}},[e("i",{staticClass:"material-icons icon",attrs:{"aria-hidden":"true"}},[t._v("first_page")])])]),t._v(" "),e("li",{staticClass:"page-item",class:t.classFirstPage},[e("button",{staticClass:"pagination__navigation",class:t.classFirstPage,staticStyle:{"border-left":"0"},on:{click:function(a){return a.preventDefault(),t.lastPage(a)}}},[e("i",{staticClass:"material-icons icon",attrs:{"aria-hidden":"true"}},[t._v("chevron_left")])])]),t._v(" "),e("li",{staticClass:"page-item",staticStyle:{"margin-right":"0"}},[e("button",{staticClass:"pagination__navigation pagination__navigation--disabled text-bold primary--text",staticStyle:{"border-right":"0","border-left":"0"}},[t._v("\n          "+t._s(t.titlePage)+"\n        ")])]),t._v(" "),e("li",{staticClass:"page-item",class:t.classLastPage,staticStyle:{"margin-right":"0"}},[e("button",{staticClass:"pagination__navigation",class:t.classLastPage,staticStyle:{"border-left":"0"},on:{click:function(a){return a.preventDefault(),t.nextPage(a)}}},[e("i",{staticClass:"material-icons icon",attrs:{"aria-hidden":"true"}},[t._v("chevron_right")])])]),t._v(" "),e("li",{staticClass:"page-item",class:t.classLastPage},[e("button",{staticClass:"pagination__navigation",class:t.classLastPage,on:{click:function(a){return a.preventDefault(),t.nextPageLast(a)}}},[e("i",{staticClass:"material-icons icon",attrs:{"aria-hidden":"true"}},[t._v("last_page")])])])])]):t._e()])},staticRenderFns:[]}},609:function(t,a,e){"use strict";Object.defineProperty(a,"__esModule",{value:!0});var s=e(236),n=e.n(s),i=e(39);a.default={props:["index","id"],components:{"tiny-pagination":n.a},data:function(){return{tinhTrangHoSo:[{status:"trangThai1",statusName:"trang thai 1",count:"10"},{status:"trangThai2",statusName:"trang thai 2",count:"8"},{status:"trangThai3",statusName:"trang thai 3",count:"9"}],dangThuLy:[{dueCode:"trangThai1",dueName:"trang thai 1",count:"10"},{dueCode:"trangThai2",dueName:"trang thai 2",count:"8"},{dueCode:"trangThai3",dueName:"trang thai 3",count:"9"}],soTheoDoi:[{register:"trangThai1",registerName:"trang thai 1",count:"10"},{register:"trangThai2",registerName:"trang thai 2",count:"8"},{register:"trangThai3",registerName:"trang thai 3",count:"9"}],headers:[{text:"STT",align:"center",sortable:!1,class:"stt_column"},{text:"Mã hồ sơ",align:"center",sortable:!1,class:"mahoso_column"},{text:"Thủ tục",align:"center",sortable:!1,class:"thutuc_column"},{text:"Chủ hồ sơ",align:"center",sortable:!1,class:"chuhoso_column"},{text:"Địa chỉ",align:"center",sortable:!1,class:"diachi_column"},{text:"Ngày tiếp nhận",align:"center",sortable:!1,class:"ngaytiepnhan_column"},{text:"Ngày hẹn trả",align:"center",sortable:!1,class:"ngayhentra_column"},{text:"Ngày gửi",align:"center",sortable:!1,class:"ngaygui_column"},{text:"Ngày trả kết quả",align:"center",sortable:!1,class:"ngaytraketqua_column"}],statusSearch:null,dueSearch:null,registerSearch:null,traCuuDatas:[],traCuuDatasTotal:0,traCuuDatasPage:1,keywordSearch:""}},computed:{loading:function(){return this.$store.getters.loading}},watch:{},created:function(){var t=this;t.$nextTick(function(){var a=t.$router.history.current.query;a.hasOwnProperty("page")&&1!==a.page?t.traCuuDatasPage=a.page:t.traCuuDatasPage=1})},methods:{initData:function(t){var a=this;a.$store.dispatch("loadDossierCounting",{}).then(function(t){a.tinhTrangHoSo=t.hasOwnProperty("bystatus")?t.bystatus:[],a.dangThuLy=t.hasOwnProperty("bydue")?t.bydue:[],a.soTheoDoi=t.hasOwnProperty("byregister")?t.byregister:[]}),a.doLoadingDataHoSo()},goBack:function(){window.history.back()},quickSearch:function(){this.doLoadingDataHoSo()},advancedSearch:function(){this.doLoadingDataHoSo()},changeTinhTrangHoSo:function(t){this.statusSearch=t.status},changeDangThuLy:function(t){this.dueSearch=t.dueCode},changeSoTheoDoi:function(t){this.registerSearch=t.register},paggingDataTraCuu:function(t){var a=this,e=a.$router.history.current,s=e.query,n="?";s.page="";for(var i in s)""!==s[i]&&"undefined"!==s[i]&&void 0!==s[i]&&(n+=i+"="+s[i]+"&");n+="page="+t.page,a.$router.push({path:e.path+n})},doLoadingDataHoSo:function(){var t=this,a=i.a.history.current.query;console.log(a);var e={queryParams:"/o/rest/v2/dossiers",page:t.traCuuDatasPage,agency:t.govAgencyCode,service:t.serviceCode,template:t.templateNo,status:t.statusSearch,dueCode:t.dueSearch,register:t.registerSearch,keyword:t.keywordSearch};t.$store.dispatch("loadingDataHoSo",e).then(function(a){t.traCuuDatas=a.data,t.traCuuDatasTotal=a.total})}},filters:{dateTimeView:function(t){if(t){var a=new Date(t);return a.getDate().toString().padStart(2,"0")+"/"+(a.getMonth()+1).toString().padStart(2,"0")+"/"+a.getFullYear()+" | "+a.getHours().toString().padStart(2,"0")+":"+a.getMinutes().toString().padStart(2,"0")}return""}}}},616:function(t,a){t.exports={render:function(){var t=this,a=t.$createElement,e=t._self._c||a;return e("div",[e("v-layout",{attrs:{row:"",wrap:""}},[e("v-flex",{staticClass:"mb-2",attrs:{xs12:"",sm12:""}},[e("v-subheader",{staticStyle:{"font-weight":"bold"}},[t._v("Tình trạng hồ sơ")]),t._v(" "),t._l(t.tinhTrangHoSo,function(a,s){return e("v-btn",{key:a.status,class:{active:a.status===t.statusSearch},attrs:{color:"primary",outline:""},on:{click:function(e){t.changeTinhTrangHoSo(a)}}},[t._v(t._s(a.statusName)+" ("+t._s(a.count)+")")])})],2),t._v(" "),e("v-flex",{staticClass:"mb-2",attrs:{xs12:"",sm12:""}},[e("v-subheader",{staticStyle:{"font-weight":"bold"}},[t._v("Đang thụ lý")]),t._v(" "),t._l(t.dangThuLy,function(a,s){return e("v-btn",{key:a.dueCode,class:{active:a.dueCode===t.dueSearch},attrs:{color:"primary",outline:""},on:{click:function(e){t.changeDangThuLy(a)}}},[t._v(t._s(a.dueName)+" ("+t._s(a.count)+")")])})],2),t._v(" "),e("v-flex",{staticClass:"mb-2",attrs:{xs12:"",sm12:""}},[e("v-subheader",{staticStyle:{"font-weight":"bold"}},[t._v("Sổ theo dõi")]),t._v(" "),t._l(t.soTheoDoi,function(a,s){return e("v-btn",{key:a.register,class:{active:a.register===t.registerSearch},attrs:{color:"primary",outline:""},on:{click:function(e){t.changeSoTheoDoi(a)}}},[t._v(t._s(a.registerName)+" ("+t._s(a.count)+")")])})],2),t._v(" "),e("v-flex",{attrs:{xs12:"",sm9:""}},[e("v-text-field",{staticClass:"ml-3",attrs:{label:"Nhập từ khóa"},model:{value:t.keywordSearch,callback:function(a){t.keywordSearch=a},expression:"keywordSearch"}})],1),t._v(" "),e("v-flex",{attrs:{xs12:"",sm3:""}},[e("v-btn",{staticClass:"ml-4",attrs:{color:"primary"},on:{click:t.quickSearch}},[t._v("Tìm kiếm nhanh")])],1),t._v(" "),e("v-flex",{attrs:{xs12:"",sm9:""}}),t._v(" "),e("v-flex",{attrs:{xs12:"",sm3:""}},[e("v-btn",{staticClass:"ml-4 mb-3",attrs:{color:"primary"},on:{click:t.advancedSearch}},[t._v("Tìm kiếm nâng cao")])],1),t._v(" "),e("v-flex",{attrs:{xs12:"",sm12:""}},[e("v-data-table",{staticClass:"table-landing table-bordered",attrs:{headers:t.headers,items:t.traCuuDatas,"hide-actions":"","no-data-text":"Không có dữ liệu"},scopedSlots:t._u([{key:"headerCell",fn:function(a){return[e("v-tooltip",{attrs:{bottom:""}},[e("span",{attrs:{slot:"activator"},slot:"activator"},[t._v("\n            "+t._s(a.header.text)+"\n          ")]),t._v(" "),e("span",[t._v("\n            "+t._s(a.header.text)+"\n          ")])])]}},{key:"items",fn:function(a){return[e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.dossierNo)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.serviceName)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.applicantName)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.address)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.receiveDate)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.releaseDate)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.submitDate)+"\n        ")]),t._v(" "),e("td",{staticClass:"text-xs-center"},[t._v("\n          "+t._s(a.item.finishDate)+"\n        ")])]}}])}),t._v(" "),e("div",{staticClass:"text-xs-right layout wrap",staticStyle:{position:"relative"}},[e("div",{staticClass:"flex pagging-table px-2"},[e("tiny-pagination",{attrs:{total:t.traCuuDatasTotal,page:t.traCuuDatasPage,"custom-class":"custom-tiny-class"},on:{"tiny:change-page":t.paggingDataTraCuu}})],1)])],1)],1)],1)},staticRenderFns:[]}}});
//# sourceMappingURL=9.4a4aeb9f9f396121bd5f.js.map