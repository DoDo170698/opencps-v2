webpackJsonp([2],{459:function(t,e,a){var o=a(182)(a(467),a(470),null,null);o.options.__file="d:\\FDS_OPENCPS\\front-end-v2.1\\frontend-opencps-v2.1\\onegate_21_kios\\src\\components\\TraCuuHoSo.vue",o.esModule&&Object.keys(o.esModule).some(function(t){return"default"!==t&&"__esModule"!==t})&&console.error("named exports are not supported in *.vue files."),o.options.functional&&console.error("[vue-loader] TraCuuHoSo.vue: functional components are not supported with templates, they should use render functions."),t.exports=o.exports},467:function(t,e,a){"use strict";Object.defineProperty(e,"__esModule",{value:!0});var o=a(80),s=a(60);a.n(s);e.default={props:[],components:{},data:function(){return{loading:!1,loadingAction:!1,loadingTable:!1,dossierNoKey:"",applicantNameKey:"",applicantIdNoKey:"",dossierList:[],dossierItemTotal:0,hosoDatasPage:1,totalPages:1,headersTable:[{text:"Số thứ tự",align:"center",sortable:!1},{text:"Số hồ sơ",align:"center",sortable:!1},{text:"Thủ tục thực hiện",align:"center",sortable:!1},{text:"Người nộp",align:"center",sortable:!1},{text:"Tình trạng hồ sơ",align:"center",sortable:!1}]}},computed:{},created:function(){this.$nextTick(function(){var t=this,e=t.$router.history.current,a=e.query;t.dossierNoKey=a.hasOwnProperty("dossierNo")?a.dossierNo:"",t.applicantIdNoKey=a.hasOwnProperty("applicantIdNo")?a.applicantIdNo:"",t.applicantNameKey=a.hasOwnProperty("applicantName")?a.applicantName:"",t.hosoDatasPage=1,t.doLoadingDataHoSo()})},watch:{$route:function(t,e){var a=this,o=(t.params,t.query);a.dossierNoKey=o.hasOwnProperty("dossierNo")?o.dossierNo:"",a.applicantIdNoKey=o.hasOwnProperty("applicantIdNo")?o.applicantIdNo:"",a.applicantNameKey=o.hasOwnProperty("applicantName")?o.applicantName:"",a.hosoDatasPage=1,a.doLoadingDataHoSo()}},methods:{filterDossier:function(){var t=this,e=t.$router.history.current,a=e.query,o="?";a.dossierNo=t.dossierNoKey?t.dossierNoKey:"",a.applicantIdNo=t.applicantIdNoKey?t.applicantIdNoKey:"",a.applicantName=t.applicantNameKey?t.applicantNameKey:"";for(var s in a)""!==a[s]&&"undefined"!==a[s]&&void 0!==a[s]&&null!==a[s]&&(o+=s+"="+a[s]+"&");t.$router.push({path:e.path+o,query:{renew:Math.floor(100*Math.random())+1}})},showMore:function(){var t=this;t.hosoDatasPage+=1,t.loadingTable=!0;var e=o.a.history.current.query,a=null;a={page:t.hosoDatasPage,dossierNo:e.hasOwnProperty("dossierNo")?e.dossierNo:"",applicantName:e.hasOwnProperty("applicantName")?e.applicantName:"",applicantIdNo:e.hasOwnProperty("applicantIdNo")?e.applicantIdNo:""},t.$store.dispatch("loadingDataHoSo",a).then(function(e){t.loadingTable=!1;var a=e.data;t.dossierList=t.dossierList.concat(a)}).catch(function(e){t.loadingTable=!1})},doLoadingDataHoSo:function(){var t=this;t.dossierList=[],t.loadingTable=!0;var e=o.a.history.current.query,a=null;a={page:t.hosoDatasPage,dossierNo:e.hasOwnProperty("dossierNo")?e.dossierNo:"",applicantName:e.hasOwnProperty("applicantName")?e.applicantName:"",applicantIdNo:e.hasOwnProperty("applicantIdNo")?e.applicantIdNo:""},t.$store.dispatch("loadingDataHoSo",a).then(function(e){t.loadingTable=!1,t.dossierList=e.data,t.dossierItemTotal=e.total,t.totalPages=Math.ceil(e.total/15)}).catch(function(e){t.loadingTable=!1,t.dossierList=[],t.dossierItemTotal=0})},viewDetail:function(t){o.a.push("/tra-cuu-ho-so/"+t.dossierId)}}}},470:function(t,e,a){t.exports={render:function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",{staticClass:"px-2 py-0"},[a("div",[a("v-card",[a("v-layout",{staticClass:"px-0 py-0",attrs:{wrap:""}},[a("div",{staticStyle:{width:"calc(100% - 150px)"}},[a("v-layout",{attrs:{wrap:""}},[a("v-flex",{staticClass:"pl-2 pr-2",attrs:{xs4:""}},[a("v-text-field",{attrs:{label:"Số hồ sơ",placeholder:"Nhấn để nhập mã số hồ sơ",clearable:""},model:{value:t.dossierNoKey,callback:function(e){t.dossierNoKey=e},expression:"dossierNoKey"}})],1),t._v(" "),a("v-flex",{staticClass:"pl-2 pr-2",attrs:{xs4:""}},[a("v-text-field",{attrs:{label:"Số CMND",placeholder:"Nhấn để nhập số CMND",clearable:""},model:{value:t.applicantIdNoKey,callback:function(e){t.applicantIdNoKey=e},expression:"applicantIdNoKey"}})],1),t._v(" "),a("v-flex",{staticClass:"pl-2 pr-2",attrs:{xs4:""}},[a("v-text-field",{attrs:{label:"Họ tên người nộp",placeholder:"Nhấn để nhập họ và tên",clearable:""},model:{value:t.applicantNameKey,callback:function(e){t.applicantNameKey=e},expression:"applicantNameKey"}})],1)],1)],1),t._v(" "),a("div",{staticClass:"pt-2 text-center",staticStyle:{width:"150px"}},[a("v-btn",{attrs:{color:"primary",loading:t.loadingTable,disabled:t.loadingTable},on:{click:t.filterDossier}},[a("v-icon",{attrs:{size:"18"}},[t._v("search")]),t._v("\n             \n            Tra Cứu\n            "),a("span",{attrs:{slot:"loader"},slot:"loader"},[t._v("Loading...")])],1)],1)]),t._v(" "),a("div",{staticClass:"my-3 pt-2 text-center",staticStyle:{height:"40px","background-color":"#d6e9f7"}},[a("span",{staticStyle:{color:"red"}},[t._v("Có "+t._s(t.dossierItemTotal)+" kết quả được tìm thấy")])]),t._v(" "),a("v-data-table",{staticClass:"table-landing table-bordered",attrs:{headers:t.headersTable,items:t.dossierList,"hide-actions":""},scopedSlots:t._u([{key:"items",fn:function(e){return[a("tr",{class:{active:e.index%2==1},on:{click:function(a){t.viewDetail(e.item)}}},[a("td",{staticClass:"text-xs-center"},[t.loadingTable?a("content-placeholders",[a("content-placeholders-text",{attrs:{lines:1}})],1):a("div",[a("span",[t._v(t._s(e.index+1))])])],1),t._v(" "),a("td",{staticClass:"text-xs-left"},[t.loadingTable?a("content-placeholders",[a("content-placeholders-text",{attrs:{lines:1}})],1):a("div",[a("span",[t._v(t._s(e.item.dossierNo))]),a("br")])],1),t._v(" "),a("td",{staticClass:"text-xs-left"},[t.loadingTable?a("content-placeholders",[a("content-placeholders-text",{attrs:{lines:1}})],1):a("div",[a("span",[t._v(t._s(e.item.serviceName))])])],1),t._v(" "),a("td",{staticClass:"text-xs-left"},[t.loadingTable?a("content-placeholders",[a("content-placeholders-text",{attrs:{lines:1}})],1):a("div",[a("span",[t._v(t._s(e.item.applicantName))])])],1),t._v(" "),a("td",{staticClass:"text-xs-left"},[t.loadingTable?a("content-placeholders",[a("content-placeholders-text",{attrs:{lines:1}})],1):a("div",[a("span",[a("span",[t._v(t._s(e.item.dossierStatusText))])])])],1)])]}}])}),t._v(" "),t.hosoDatasPage<t.totalPages&&t.dossierItemTotal>0?a("div",{staticClass:"mt-3 text-center"},[a("v-btn",{attrs:{outline:"",color:"indigo",loading:t.loadingTable,disabled:t.loadingTable},on:{click:t.showMore}},[a("v-icon",{attrs:{size:"20"}},[t._v("expand_more")]),t._v(" \n          Xem thêm\n          "),a("span",{attrs:{slot:"loader"},slot:"loader"},[t._v("Loading...")])],1)],1):t._e()],1)],1)])},staticRenderFns:[]},t.exports.render._withStripped=!0}});
//# sourceMappingURL=2.5a2d4903026d445305f5.js.map