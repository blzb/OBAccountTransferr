/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  _________
 ************************************************************************
 */

// put within a function to hide local vars etc.

 
// Define a class that extends OBPopup
isc.defineClass('Lucasian_ParameterPopup', isc.OBPopup);

 
isc.Lucasian_ParameterPopup.addProperties({
       width: 320,
       height: 200,
       title: OB.I18N.getLabel('LLM_transfer'),
       showMinimizeButton: false,
       showMaximizeButton: false,
  
       view: null,
       params: null,
       actionHandler: null,
       orders: null,
  
       mainform: null,
       okButton: null,
       cancelButton: null,
  
       initWidget: function () {
	
              // Form that contains the parameters
              var origen = this.origen;
              var destino = this.destino;
              var view = this.view;
              console.log(this);
              var valores = {};
              valores[origen[OB.Constants.ID]] = origen["name"],
              valores[destino[OB.Constants.ID]]= destino["name"];
              
              glitemDS = OB.Datasource.create({ 
                     destroy: function () { 
                            this.Super('destroy', arguments); 
                     }, 
                     dataURL: OB.Application.contextUrl + 'org.openbravo.service.datasource/FinancialMgmtGLItem' 
              }); 
             
              
              console.log(valores);
              var datos = {
                     showErrorIcons : true,
                     numCols: 1,
                     errorOrientation : 'left',
                     fields: [
                     { 
                            title: OB.I18N.getLabel('LLM_glItemTitle'),
                            name:"glItem", 
                            type: 'select',// equal to select in smartclient type 
                            optionDataSource: glitemDS, 
                            displayField: 'name', 
                            valueField: 'id', 
                            required:true,	
                            autoFetchData: true ,
                            titleClassName: 'OBFormFieldLabel', 
                            textBoxStyle: 'OBFormFieldSelectInput', 
                            cellClassName: 'OBFormField'
                     },
                     {
                            name: 'monto',
                            title: OB.I18N.getLabel('LLM_montoTitle'),
                            height: 20,
                            required:true,	
                            width: 200,
                            type: 'text' //Date reference
                     },
                     {
                            name: 'comentario',
                            title: OB.I18N.getLabel('LLM_comentarioTitle'),
                            required:true,	
                            height: 20,
                            width: 200,
                            type: 'text' //Date reference
                     },
                     {
                            name: 'origen',
                            title: OB.I18N.getLabel('LLM_origenTitle'),
                            required:true,	
                            height: 20,
                            width: 200,
                            type: 'select',
                            valueMap:valores,
                            defaultValue: origen[OB.Constants.ID],
                            titleClassName: 'OBFormFieldLabel', 
                            textBoxStyle: 'OBFormFieldSelectInput', 
                            cellClassName: 'OBFormField'
                            
                     } 
                     ,
                     {
                            name: 'destino',
                            title: OB.I18N.getLabel('LLM_destinoTitle'),
                            required:true,	
                            height: 20,
                            width: 200,
                            type: 'select',
                            valueMap: valores,
                            defaultValue: destino[OB.Constants.ID],
                            titleClassName: 'OBFormFieldLabel', 
                            textBoxStyle: 'OBFormFieldSelectInput', 
                            cellClassName: 'OBFormField'
                     }  
                     ]
              };
              console.log(datos)
              this.mainform = isc.DynamicForm.create(datos);
              console.log(view);
              // OK Button
              this.okButton = isc.OBFormButton.create({
                     title: OB.I18N.getLabel('LLM_OK_BUTTON_TITLE'),
                     popup: this,
                     action: function () {
                            if(this.popup.mainform.validate()){
                                   var callback = function (rpcResponse, data, rpcRequest) {
                                          // show result
                                          console.log(data);
                                          isc.say(OB.I18N.getLabel(data.label));
 
                                          // close process to refresh current view
                                          rpcRequest.clientContext.popup.closeClick();
                                          view.viewGrid.refreshGrid();
                                   };
                            
                                   OB.RemoteCallManager.call(this.popup.actionHandler, {
                                          origen:this.popup.mainform.getField("origen").getValue(),
                                          destino: this.popup.mainform.getField("destino").getValue(),
                                          monto: this.popup.mainform.getField("monto").getValue(),
                                          comentario: this.popup.mainform.getField("comentario").getValue(),
                                          glItem: this.popup.mainform.getField("glItem").getValue()
                                   //action: this.popup.params.action,
                                   //dateParam: this.popup.mainform.getField('Date').getValue(), //send the parameter to the server too
                                   }, {}, callback, {
                                          popup: this.popup
                                   }); 
                            }
                     }
              });
   
              // Cancel Button
              this.cancelButton = isc.OBFormButton.create({
                     title: OB.I18N.getLabel('LLM_CANCEL_BUTTON_TITLE'),
                     popup: this,
                     action: function () {
                            this.popup.closeClick();
                     }
              }); 
   
              //Add the elements into a layout   
              this.items = [
              isc.VLayout.create({
                     defaultLayoutAlign: "center",
                     align: "center",
                     width: "100%",
                     layoutMargin: 10,
                     membersMargin: 6,
                     members: [
                     isc.HLayout.create({
                            defaultLayoutAlign: "center",
                            align: "center",
                            layoutMargin: 30,
                            membersMargin: 6,
                            members: this.mainform
                     }), 
                     isc.HLayout.create({
                            defaultLayoutAlign: "center",
                            align: "center",
                            membersMargin: 10,
                            members: [this.okButton, this.cancelButton]
                     })
                     ]
              })
              ];
   
              this.Super('initWidget', arguments);
       }
 
});

(function () {
       var buttonProps = {
              action: function(){
                     var callback, orders = [],
                     i, view = this.view,
                     grid = view.viewGrid,
                     selectedRecords = grid.getSelectedRecords();
                     console.log(selectedRecords);
                     // collect the order ids
                     for (i = 0; i < selectedRecords.length; i++) {
                            orders.push(selectedRecords[i][OB.Constants.ID]);
                     }

                     // define the callback function which shows the result to the user
                     callback = function (rpcResponse, data, rpcRequest) {
                            console.log(data);
                     //isc.say(OB.I18N.getLabel('Lucasian_SumResult', [data.total]));
                     };

                     // and call the server
                     /*
                     OB.RemoteCallManager.call('com.lucasian.openbravo.transfer.TransferActionHandler', {
                            cuentas: orders
                     }, {}, callback);
                      */
                    
                     isc.Lucasian_ParameterPopup.create({
                            origen: selectedRecords[0],
                            destino: selectedRecords[1],
                            view: view,
                            //params: params,
                            actionHandler: 'com.lucasian.openbravo.transfer.TransferActionHandler'
                     }).show();
              },
              buttonType: 'llm_transfer',
              prompt: OB.I18N.getLabel('LLM_transfer'),
              updateState: function(){
                     var view = this.view, 
                     form = view.viewForm, 
                     grid = view.viewGrid, 
                     selectedRecords = grid.getSelectedRecords();
          
                     this.setDisabled(!(selectedRecords.length === 2));
          
              }
       };
  
       // register the button for the sales order tab
       // the first parameter is a unique identification so that one button can not be registered multiple times.
       OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 100, null);
}());