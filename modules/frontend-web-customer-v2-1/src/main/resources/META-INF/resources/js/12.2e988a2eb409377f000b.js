webpackJsonp([12],{223:function(e,t,n){n(540);var o=n(146)(n(535),n(542),"data-v-7cc31a58",null);e.exports=o.exports},535:function(e,t,n){"use strict";Object.defineProperty(t,"__esModule",{value:!0});var o=n(149),s=n.n(o),c={colorChecked:"#75C791",colorUnchecked:"#bfcbd9",cssColors:!1,labelChecked:"on",labelUnchecked:"off",width:50,height:22,margin:3},i=function(e,t){return"object"===(void 0===e?"undefined":s()(e))&&e.hasOwnProperty(t)},l=function(e){return e+"px"};t.default={name:"toggle-button",props:{title_checked:"",title_unchecked:"",value:{type:Boolean,default:!1},disabled:{type:Boolean,default:!1},sync:{type:Boolean,default:!1},speed:{type:Number,default:300},color:{type:[String,Object],validator:function(e){return"object"===(void 0===e?"undefined":s()(e))?e.checked||e.unchecked:"string"==typeof e}},cssColors:{type:Boolean,default:!1},labels:{type:[Boolean,Object],default:!1,validator:function(e){return"object"===(void 0===e?"undefined":s()(e))?e.checked||e.unchecked:"boolean"==typeof e}},height:{type:Number,default:c.height},width:{type:Number,default:c.width}},computed:{className:function(){return["vue-js-switch",{toggled:this.toggled,disabled:this.disabled}]},ariaChecked:function(){return this.toggled.toString()},coreStyle:function(){return{width:l(this.width),height:l(this.height),backgroundColor:this.cssColors?null:this.colorCurrent,borderRadius:l(Math.round(this.height/2))}},maxWidth:function(){return{width:l(this.width)+"!important"}},buttonRadius:function(){return this.height-2*c.margin},distance:function(){return l(this.width-this.height+c.margin)},buttonStyle:function(){return{width:l(this.buttonRadius),height:l(this.buttonRadius),transition:"transform "+this.speed+"ms",transform:this.toggled?"translate3d("+this.distance+", 3px, 0px)":null}},labelStyle:function(){return{lineHeight:l(this.height)}},colorChecked:function(){var e=this.color;return"object"!==(void 0===e?"undefined":s()(e))?e||c.colorChecked:i(e,"checked")?e.checked:c.colorChecked},colorUnchecked:function(){var e=this.color;return i(e,"unchecked")?e.unchecked:c.colorUnchecked},colorCurrent:function(){return this.toggled?this.colorChecked:this.colorUnchecked},labelChecked:function(){return i(this.labels,"checked")?this.labels.checked:c.labelChecked},labelUnchecked:function(){return i(this.labels,"unchecked")?this.labels.unchecked:c.labelUnchecked}},watch:{value:function(e){this.sync&&(this.toggled=!!e)}},data:function(){return{toggled:!!this.value}},methods:{toggle:function(e){this.toggled=!this.toggled,this.$emit("input",this.toggled),this.$emit("change",{value:this.toggled,srcEvent:e})}}}},538:function(e,t,n){t=e.exports=n(529)(!0),t.push([e.i,".vue-js-switch[data-v-7cc31a58]{display:inline-block;position:relative;overflow:hidden;vertical-align:middle;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none;font-size:10px;cursor:pointer}.vue-js-switch .v-switch-input[data-v-7cc31a58]{display:none}.vue-js-switch .v-switch-label[data-v-7cc31a58]{position:absolute;top:0;font-weight:600;color:#fff}.vue-js-switch .v-switch-label.v-left[data-v-7cc31a58]{left:10px}.vue-js-switch .v-switch-label.v-right[data-v-7cc31a58]{right:10px}.vue-js-switch .v-switch-core[data-v-7cc31a58]{display:block;position:relative;box-sizing:border-box;outline:0;margin:0;transition:border-color .3s,background-color .3s;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.vue-js-switch .v-switch-core .v-switch-button[data-v-7cc31a58]{display:block;position:absolute;overflow:hidden;top:0;left:0;transform:translate3d(3px,3px,0);border-radius:100%;background-color:#fff}.vue-js-switch.disabled[data-v-7cc31a58]{pointer-events:none;opacity:1}","",{version:3,sources:["/home/hoanganh/Pictures/FrontEnd_Opencps2.1/frontend-opencps-v2.1/onegate_21_fe/src/components/toggleButton.vue"],names:[],mappings:"AACA,gCACE,qBAAsB,AACtB,kBAAmB,AACnB,gBAAiB,AACjB,sBAAuB,AACvB,yBAA0B,AACvB,sBAAuB,AACtB,qBAAsB,AAClB,iBAAkB,AAC1B,eAAgB,AAChB,cAAe,CAChB,AACD,gDACE,YAAa,CACd,AACD,gDACE,kBAAmB,AACnB,MAAO,AACP,gBAAiB,AACjB,UAAY,CACb,AACD,uDACE,SAAU,CACX,AACD,wDACE,UAAW,CACZ,AACD,+CACE,cAAe,AACf,kBAAmB,AACnB,sBAAuB,AACvB,UAAW,AACX,SAAU,AACV,iDAAqD,AACrD,yBAA0B,AACvB,sBAAuB,AACtB,qBAAsB,AAClB,gBAAiB,CAC1B,AACD,gEACE,cAAe,AACf,kBAAmB,AACnB,gBAAiB,AACjB,MAAO,AACP,OAAQ,AACR,iCAAoC,AACpC,mBAAoB,AACpB,qBAAsB,CACvB,AACD,yCACE,oBAAqB,AACrB,SAAU,CACX",file:"toggleButton.vue",sourcesContent:["\n.vue-js-switch[data-v-7cc31a58] {\n  display: inline-block;\n  position: relative;\n  overflow: hidden;\n  vertical-align: middle;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none;\n  font-size: 10px;\n  cursor: pointer\n}\n.vue-js-switch .v-switch-input[data-v-7cc31a58] {\n  display: none\n}\n.vue-js-switch .v-switch-label[data-v-7cc31a58] {\n  position: absolute;\n  top: 0;\n  font-weight: 600;\n  color: white\n}\n.vue-js-switch .v-switch-label.v-left[data-v-7cc31a58] {\n  left: 10px\n}\n.vue-js-switch .v-switch-label.v-right[data-v-7cc31a58] {\n  right: 10px\n}\n.vue-js-switch .v-switch-core[data-v-7cc31a58] {\n  display: block;\n  position: relative;\n  box-sizing: border-box;\n  outline: 0;\n  margin: 0;\n  transition: border-color 0.3s, background-color 0.3s;\n  -webkit-user-select: none;\n     -moz-user-select: none;\n      -ms-user-select: none;\n          user-select: none\n}\n.vue-js-switch .v-switch-core .v-switch-button[data-v-7cc31a58] {\n  display: block;\n  position: absolute;\n  overflow: hidden;\n  top: 0;\n  left: 0;\n  transform: translate3d(3px, 3px, 0);\n  border-radius: 100%;\n  background-color: #fff\n}\n.vue-js-switch.disabled[data-v-7cc31a58] {\n  pointer-events: none;\n  opacity: 1\n}\n"],sourceRoot:""}])},540:function(e,t,n){var o=n(538);"string"==typeof o&&(o=[[e.i,o,""]]),o.locals&&(e.exports=o.locals);n(530)("120c24d1",o,!0)},542:function(e,t){e.exports={render:function(){var e=this,t=e.$createElement,n=e._self._c||t;return n("label",{class:e.className,style:e.maxWidth,attrs:{role:"checkbox",title:e.toggled?e.title_checked:e.title_unchecked,"aria-checked":e.ariaChecked}},[n("input",{staticClass:"v-switch-input",attrs:{type:"checkbox"},on:{change:function(t){return t.stopPropagation(),e.toggle(t)}}}),e._v(" "),n("div",{staticClass:"v-switch-core",style:e.coreStyle},[n("div",{staticClass:"v-switch-button",style:e.buttonStyle})]),e._v(" "),e.labels?[e.toggled?n("span",{staticClass:"v-switch-label v-left",style:e.labelStyle,domProps:{innerHTML:e._s(e.labelChecked)}}):n("span",{staticClass:"v-switch-label v-right",style:e.labelStyle,domProps:{innerHTML:e._s(e.labelUnchecked)}})]:e._e()],2)},staticRenderFns:[]}}});
//# sourceMappingURL=12.2e988a2eb409377f000b.js.map