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
 * All portions are Copyright (C) 2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  __________
 ************************************************************************
 */
package com.lucasian.openbravo.transfer;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.project.Project;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Sums the orders passed in through a json array and returns the result.
 *
 * This class is used as an example in howtos in the Openbravo Developers Guide:
 * http://wiki.openbravo.com/wiki/Category:Developers_Guide
 *
 * @author mtaal
 */
public class TransferActionHandler extends BaseActionHandler {

       protected JSONObject execute(Map<String, Object> parameters, String data) {
              JSONObject json = new JSONObject();
              AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
              try {

                     // get the data as json
                     final JSONObject jsonData = new JSONObject(data);
                     BigDecimal monto = BigDecimal.valueOf(Double.parseDouble(jsonData.getString("monto")));
                     String comentario = jsonData.getString("comentario");
                     String origenId = jsonData.getString("origen");
                     String destinoId = jsonData.getString("destino");
                     String strGLItemId = jsonData.getString("glItem");
                     
                     FIN_FinancialAccount origen = OBDal.getInstance().get(FIN_FinancialAccount.class, origenId);
                     FIN_FinancialAccount destino = OBDal.getInstance().get(FIN_FinancialAccount.class, destinoId);

                     HttpServletRequest request = (HttpServletRequest) parameters.get(KernelConstants.HTTP_REQUEST);
                     VariablesSecureApp vars = new VariablesSecureApp(request);

                     dao = new AdvPaymentMngtDao();
                     String strMessage = "";
                     OBError msg = new OBError();
                     OBContext.setAdminMode();
                            // SALES = DEPOSIT
                            // PURCHASE = PAYMENT                            
                            // Accounting Dimensions
                            final String strElement_OT = "";
                            final String strElement_PJ = "";
                            final Project project = OBDal.getInstance().get(Project.class, strElement_PJ);
                            final String strElement_AY = "";
                            final ABCActivity activity = OBDal.getInstance().get(ABCActivity.class, strElement_AY);

                            final String strElement_MC = "";
                            final Campaign campaign = OBDal.getInstance().get(Campaign.class, strElement_MC);

                            GLItem glItem = OBDal.getInstance().get(GLItem.class, strGLItemId);
                            String description = comentario;

                            // Currency, Organization, paymentDate,
                            FIN_FinaccTransaction transaccionRetiro = dao.getNewFinancialTransaction(
                                    origen.getOrganization() ,
                                    origen,
                                    TransactionsDao.getTransactionMaxLineNo(origen) + 10,
                                    null,
                                    description,
                                    new Date(),
                                    glItem,
                                    "PWNC",
                                    new BigDecimal(0.0),
                                    monto,
                                    project,
                                    campaign,
                                    activity,
                                    "BPW",
                                    new Date(),
                                    null,
                                    null,
                                    null);
                            FIN_FinaccTransaction transaccionDeposito = dao.getNewFinancialTransaction(
                                    destino.getOrganization()),
                                    destino,
                                    TransactionsDao.getTransactionMaxLineNo(destino) + 10,
                                    null,
                                    description,
                                    new Date(),
                                    glItem,
                                    "RDNC",
                                    monto,
                                    new BigDecimal(0.0),
                                    project,
                                    campaign,
                                    activity,
                                    "BPD",
                                    new Date(),
                                    null,
                                    null,
                                    null);
                            
                             OBError processTransactionErrorRetiro = processTransaction(vars, new DalConnectionProvider(), "P", transaccionRetiro);
                             if (processTransactionErrorRetiro != null && "Error".equals(processTransactionErrorRetiro.getType())) {
                             throw new OBException(processTransactionErrorRetiro.getMessage());
                             }
                             OBError processTransactionErrorDeposito = processTransaction(vars, new DalConnectionProvider(), "P", transaccionDeposito);
                             if (processTransactionErrorDeposito != null && "Error".equals(processTransactionErrorDeposito.getType())) {
                             throw new OBException(processTransactionErrorDeposito.getMessage());
                             }
                             strMessage = "1 " + "@RowsInserted@";
                             /*
                             if (!"".equals(strFinBankStatementLineId)) {
                             matchBankStatementLine(vars, finTrans, strFinBankStatementLineId);
                             }
                             */
                     // start with zero
                     // create the result
                     // and return it
                     json.put("label", "LLM_transferCompleted");
              } catch (Exception e) {
                     e.printStackTrace();
                     try {
                            json.put("label", "LLM_parseError");
                     } catch (Exception ex) {
                            ex.printStackTrace();
                     }
              }finally {
                            OBContext.restorePreviousMode();
                     }
              return json;
       }

       private OBError processTransaction(VariablesSecureApp vars, ConnectionProvider conn,
               String strAction, FIN_FinaccTransaction transaction) throws Exception {
              ProcessBundle pb = new ProcessBundle("F68F2890E96D4D85A1DEF0274D105BCE", vars).init(conn);
              HashMap<String, Object> parameters = new HashMap<String, Object>();
              parameters.put("action", strAction);
              parameters.put("Fin_FinAcc_Transaction_ID", transaction.getId());
              pb.setParams(parameters);
              OBError myMessage = null;
              new FIN_TransactionProcess().execute(pb);
              myMessage = (OBError) pb.getResult();
              return myMessage;
       }

       private void matchBankStatementLine(VariablesSecureApp vars, FIN_FinaccTransaction finTrans,
               String strFinBankStatementLineId, AdvPaymentMngtDao dao) {
              FIN_BankStatementLine bsline = dao.getObject(FIN_BankStatementLine.class,
                      strFinBankStatementLineId);
              // The amounts must match
              if (bsline.getCramount().compareTo(finTrans.getDepositAmount()) != 0
                      || bsline.getDramount().compareTo(finTrans.getPaymentAmount()) != 0) {
                     vars.setSessionValue("AddTransaction|ShowJSMessage", "Y");
                     vars.setSessionValue("AddTransaction|SelectedTransaction", finTrans.getId());
              } else {
                     FIN_Reconciliation reconciliation = TransactionsDao.getLastReconciliation(
                             finTrans.getAccount(), "N");
                     bsline.setMatchingtype("AD");
                     bsline.setFinancialAccountTransaction(finTrans);
                     if (finTrans.getFinPayment() != null) {
                            bsline.setBusinessPartner(finTrans.getFinPayment().getBusinessPartner());
                            finTrans.getFinPayment().setStatus("RPPC");
                     }
                     finTrans.setReconciliation(reconciliation);
                     finTrans.setStatus("RPPC");
                     OBDal.getInstance().save(bsline);
                     OBDal.getInstance().save(finTrans);
                     OBDal.getInstance().flush();
              }
       }
       
}
