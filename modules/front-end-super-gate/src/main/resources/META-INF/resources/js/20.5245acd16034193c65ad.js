webpackJsonp([20],{277:function(t,e,s){var a=s(195)(s(321),s(344),null,null);t.exports=a.exports},321:function(t,e,s){"use strict";Object.defineProperty(e,"__esModule",{value:!0}),e.default={data:function(){return{minDate:null,dataPostDossier:{serviceCode:"",govAgencyCode:"",processOptionId:""},thongTinChungHoSo:{serviceName:"",dossierTemplateName:"",serviceConfig:"",serviceOption:"",dossierNo:"",receiveDate:new Date,dueDate:(new Date).toString(),durationDate:"",dossierId:"",dossierIdCTN:"",dossierStatus:"",dossierStatusText:""}}},created:function(){var t=this;t.$nextTick(function(){t.minDate=t.getCurentDateTime("date")})},computed:{loading:function(){return this.$store.getters.loading},isDetail:function(){return this.$store.getters.isDetail}},watch:{},methods:{initData:function(t){var e=this;console.log(t);var s={serviceName:t.serviceName,dossierTemplateName:t.dossierTemplateName,dossierNo:t.dossierNo,receiveDate:t.receiveDate,dueDate:t.dueDate,durationDate:t.durationDate,dossierId:t.dossierId,dossierIdCTN:t.dossierIdCTN,dossierStatus:t.dossierStatus,dossierStatusText:t.dossierStatusText};e.thongTinChungHoSo=s},getCurentDateTime:function(t){var e=new Date;return"datetime"===t?e.getDate().toString().padStart(2,"0")+"/"+(e.getMonth()+1).toString().padStart(2,"0")+"/"+e.getFullYear()+" | "+e.getHours().toString().padStart(2,"0")+":"+e.getMinutes().toString().padStart(2,"0"):"date"===t?e.getFullYear()+"-"+(e.getMonth()+1).toString().padStart(2,"0")+"-"+e.getDate():void 0},getDuedate:function(){var t=this,e=new Date(t.thongTinChungHoSo.dueDate).getTime()-new Date(t.thongTinChungHoSo.receiveDate).getTime();return Math.ceil(e/1e3/60/60/24)<=0?1:Math.ceil(e/1e3/60/60/24)}},filters:{dateTimeView:function(t){if(t){var e=new Date(t);return e.getDate().toString().padStart(2,"0")+"/"+(e.getMonth()+1).toString().padStart(2,"0")+"/"+e.getFullYear()+" | "+e.getHours().toString().padStart(2,"0")+":"+e.getMinutes().toString().padStart(2,"0")}}}}},344:function(t,e){t.exports={render:function(){var t=this,e=t.$createElement,s=t._self._c||e;return s("div",[s("v-card",[s("v-card-text",[s("v-layout",{attrs:{wrap:""}},[s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("Thủ tục: ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm10:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):t._e(),t._v(" "),!1===t.loading?s("v-subheader",{staticStyle:{float:"left",height:"100%"}},[s("i",[t._v(t._s(t.thongTinChungHoSo.serviceName))])]):t._e()],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("Dịch vụ: ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm10:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):t._e(),t._v(" "),!1===t.loading?s("v-subheader",{staticStyle:{float:"left",height:"100%"}},[s("i",[t._v(t._s(t.thongTinChungHoSo.dossierTemplateName))])]):t._e()],1),t._v(" "),s("v-flex",{attrs:{xs12:""}}),t._v(" "),s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("\n            Mã hồ sơ: \n          ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticStyle:{float:"left"}},[s("i",[t._v(t._s(t.thongTinChungHoSo.dossierIdCTN))])])],1),t._v(" "),s("v-flex",{attrs:{xs12:""}}),t._v(" "),s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("\n            Mã tiếp nhận: \n          ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticStyle:{float:"left"}},[s("i",[t._v(t._s(t.thongTinChungHoSo.dossierNo))])])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("Thời gian giải quyết: ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):t._e(),t._v(" "),!t.loading&&t.thongTinChungHoSo.durationDate?s("v-subheader",{staticStyle:{float:"left"}},[s("i",[t._v(t._s(t.thongTinChungHoSo.durationDate)+" làm việc")])]):t._e()],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("Ngày giờ tiếp nhận: ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticStyle:{float:"left"}},[s("i",[t._v(t._s(t.thongTinChungHoSo.receiveDate))])])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticClass:"pl-0"},[t._v("Ngày hẹn trả: ")])],1),t._v(" "),s("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?s("content-placeholders",{staticClass:"mt-1"},[s("content-placeholders-text",{attrs:{lines:1}})],1):s("v-subheader",{staticStyle:{float:"left",height:"100%"}},[t._v("\n              "+t._s(t.thongTinChungHoSo.dueDate)+"\n          ")])],1)],1)],1)],1),t._v(" "),s("v-btn",{staticClass:"absolute__btn",attrs:{flat:""}},[t._v("\n    Hướng dẫn  \n    "),s("v-icon",[t._v("file_copy")])],1)],1)},staticRenderFns:[]}}});
//# sourceMappingURL=20.5245acd16034193c65ad.js.map