package com.example.paymentgatewayspringboot.controller;

import com.example.paymentgatewayspringboot.model.PaytmDetails;
import com.paytm.pg.merchant.CheckSumServiceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class PaymentController {

    public static final String CHECK_SUM_HASH = "CHECKSUMHASH";
    @Autowired
    private PaytmDetails paytmDetails;

    @Autowired
    private Environment env;

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping(value = "/pgredirect")
    public ModelAndView getRedirect(@RequestParam(name = "CUST_ID") String customerId,
                                    @RequestParam(name = "TXN_AMOUNT") String transactionAmount,
                                    @RequestParam(name = "ORDER_ID") String orderId) throws Exception {

        ModelAndView modelAndView = new ModelAndView("redirect:" + paytmDetails.getPaytmUrl());
        TreeMap<String, String> parameters = new TreeMap<>();
        paytmDetails.getDetails().forEach(parameters::put);
        parameters.put("MOBILE_NO", env.getProperty("paytm.mobile"));
        parameters.put("EMAIL", env.getProperty("paytm.email"));
        parameters.put("ORDER_ID", orderId);
        parameters.put("TXN_AMOUNT", transactionAmount);
        parameters.put("CUST_ID", customerId);
        String checkSum = getCheckSum(parameters);
        System.out.println("Generated checkSum = " + checkSum);
        parameters.put(CHECK_SUM_HASH, checkSum);
        modelAndView.addAllObjects(parameters);
        return modelAndView;
    }

    @PostMapping(value = "/pgresponse")
    public String getResponseRedirect(HttpServletRequest request, Model model) {

        Map<String, String[]> mapData = request.getParameterMap();
        TreeMap<String, String> parameters = new TreeMap<>();
        mapData.forEach((key, val) -> parameters.put(key, val[0]));
        String paytmChecksum = "";
        if (mapData.containsKey(CHECK_SUM_HASH)) {
            paytmChecksum = mapData.get(CHECK_SUM_HASH)[0];
            System.out.println("paytmChecksum = " + paytmChecksum);
        }
        String result;

        boolean isValideChecksum;
        System.out.println("RESULT : "+parameters.toString());
        try {
            isValideChecksum = validateCheckSum(parameters, paytmChecksum);
            if (isValideChecksum && parameters.containsKey("RESPCODE")) {
                if (parameters.get("RESPCODE").equals("01")) {
                    result = "Payment Successful";
                } else {
                    result = "Payment Failed";
                }
            } else {
                result = "Checksum mismatched";
            }
        } catch (Exception e) {
            result = e.toString();
        }
        model.addAttribute("result",result);
        parameters.remove(CHECK_SUM_HASH);
        model.addAttribute("parameters",parameters);
        return "report";
    }

    private boolean validateCheckSum(TreeMap<String, String> parameters, String paytmChecksum) throws Exception {
        return CheckSumServiceHelper.getCheckSumServiceHelper().verifycheckSum(paytmDetails.getMerchantKey(),
                parameters, paytmChecksum);
    }


    private String getCheckSum(TreeMap<String, String> parameters) throws Exception {
        return CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(paytmDetails.getMerchantKey(), parameters);
    }
}
