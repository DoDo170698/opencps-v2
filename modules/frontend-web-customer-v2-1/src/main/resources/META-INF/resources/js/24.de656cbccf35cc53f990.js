webpackJsonp([24],{235:function(t,e,a){var n=a(146)(a(576),a(578),null,null);t.exports=n.exports},576:function(t,e,a){"use strict";Object.defineProperty(e,"__esModule",{value:!0}),e.default={data:function(){return{minDate:null,editable:!1,dueDateInput:(new Date).toString(),dataPostDossier:{serviceCode:"",govAgencyCode:"",processOptionId:""},thongTinChungHoSo:{serviceName:"",dossierTemplateName:"",serviceConfig:"",serviceOption:"",dossierNo:"",receiveDate:"",dueDate:"",durationDate:"",dossierId:"",dossierIdCTN:"",dossierStatus:"",dossierStatusText:"",durationUnit:"",durationCount:""}}},created:function(){var t=this;t.$nextTick(function(){t.minDate=t.getCurentDateTime("date")})},computed:{loading:function(){return this.$store.getters.loading},isDetail:function(){return this.$store.getters.isDetail}},watch:{},methods:{initData:function(t){var e=this,a={serviceName:t.serviceName,dossierTemplateName:t.dossierTemplateName,dossierNo:t.dossierNo,receiveDate:t.editable?e.dateTimeView(t.receivingDate):t.receiveDate,dueDate:t.editable?t.receivingDuedate:t.dueDate,durationDate:t.durationDate,dossierId:t.dossierId,dossierIdCTN:t.dossierIdCTN,dossierStatus:t.dossierStatus,dossierStatusText:t.dossierStatusText,durationUnit:t.durationUnit,durationCount:t.durationCount};e.thongTinChungHoSo=a,e.editable=t.editable,e.thongTinChungHoSo.editable=e.editable,e.dueDateInput=new Date(e.thongTinChungHoSo.dueDate).toString(),e.minDate=e.getCurentDateTime("date")},getthongtinchunghoso:function(){return this.thongTinChungHoSo},getCurentDateTime:function(t){var e=new Date;return"datetime"===t?e.getDate().toString().padStart(2,"0")+"/"+(e.getMonth()+1).toString().padStart(2,"0")+"/"+e.getFullYear()+" | "+e.getHours().toString().padStart(2,"0")+":"+e.getMinutes().toString().padStart(2,"0"):"date"===t?e.getFullYear()+"-"+(e.getMonth()+1).toString().padStart(2,"0")+"-"+e.getDate():void 0},changeDate:function(){this.thongTinChungHoSo.dueDate=this.getDuedate()},getDuedate:function(){return new Date(this.dueDateInput).getTime()},durationText:function(t,e){var a;if(1===t&&e>8){var n=Math.floor(e/8)+" ngày",s=void 0;s=e%8!=0?8*(e/8-Math.floor(e/8))+" giờ":"",a=n+" "+s}else 0===t?a=e+" ngày":1===t&&e<=8&&(a=e+" giờ");return a},goBack:function(){var t=this,e=t.$router.history.current.params,a=t.$router.history.current.query;t.$router.push({path:"/danh-sach-ho-so/"+e.index,query:a})},dateTimeView:function(t){if(t){var e=new Date(t);return e.getDate().toString().padStart(2,"0")+"/"+(e.getMonth()+1).toString().padStart(2,"0")+"/"+e.getFullYear()+" "+e.getHours().toString().padStart(2,"0")+":"+e.getMinutes().toString().padStart(2,"0")}}}}},578:function(t,e){t.exports={render:function(){var t=this,e=t.$createElement,a=t._self._c||e;return a("div",[a("v-card",[a("v-card-text",[a("v-layout",{attrs:{wrap:""}},[a("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):a("v-subheader",{staticClass:"pl-0"},[t._v("\n            Mã hồ sơ: \n          ")])],1),t._v(" "),a("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):a("v-subheader",{staticClass:"text-bold",staticStyle:{float:"left"}},[a("span",{staticClass:"text-bold"},[t._v("\n              "+t._s(t.thongTinChungHoSo.dossierNo)+"\n            ")])])],1),t._v(" "),a("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):a("v-subheader",{staticClass:"pl-0"},[t._v("Thời gian giải quyết: ")])],1),t._v(" "),a("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):t._e(),t._v(" "),t.loading?t._e():a("v-subheader",{staticStyle:{float:"left"}},[a("span",{staticClass:"text-bold"},[t._v("\n              "+t._s(t.durationText(t.thongTinChungHoSo.durationUnit,t.thongTinChungHoSo.durationCount))+" làm việc\n            ")])])],1),t._v(" "),a("v-flex",{attrs:{xs12:""}}),t._v(" "),a("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):a("v-subheader",{staticClass:"pl-0"},[t._v("Ngày tiếp nhận: ")])],1),t._v(" "),a("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):a("v-subheader",{staticStyle:{float:"left"}},[a("span",{staticClass:"text-bold"},[t._v(t._s(t.thongTinChungHoSo.receiveDate))])])],1),t._v(" "),a("v-flex",{attrs:{xs12:"",sm2:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):a("v-subheader",{staticClass:"pl-0"},[t._v("Ngày hẹn trả: ")])],1),t._v(" "),a("v-flex",{attrs:{xs12:"",sm4:""}},[t.loading?a("content-placeholders",{staticClass:"mt-1"},[a("content-placeholders-text",{attrs:{lines:1}})],1):t._e(),t._v(" "),t.loading||!1!==t.editable?t._e():a("v-subheader",{staticStyle:{float:"left",height:"100%"}},[a("span",{staticClass:"text-bold"},[t._v("\n              "+t._s(t.thongTinChungHoSo.dueDate)+"\n            ")])]),t._v(" "),t.loading||!0!==t.editable?t._e():a("v-subheader",{staticStyle:{float:"left",height:"100%"}},[a("datetime",{attrs:{type:"datetime","input-format":"DD/MM/YYYY HH:mm",i18n:{ok:"Chọn",cancel:"Thoát"},"moment-locale":"vi",zone:"local","min-date":t.minDate,"monday-first":"","wrapper-class":"wrapper-datetime","auto-continue":"","auto-close":"",required:""},on:{input:t.changeDate},model:{value:t.dueDateInput,callback:function(e){t.dueDateInput=e},expression:"dueDateInput"}}),t._v(" "),a("v-icon",[t._v("event")])],1)],1)],1)],1)],1)],1)},staticRenderFns:[]}}});
//# sourceMappingURL=24.de656cbccf35cc53f990.js.map