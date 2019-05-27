package com.testpro;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.IllegalViewOperationException;

import java.math.BigDecimal;
import com.google.gson.Gson;

import kin.sdk.*;
import kin.base.xdr.Error;
import kin.base.xdr.ErrorCode;
import kin.sdk.exception.CreateAccountException;
import kin.utils.Request;
import kin.utils.ResultCallback;

public class KinNativeModule extends ReactContextBaseJavaModule {

    private KinClient kinClient;
    private KinAccount kinAccount;
    private String publicAddress;
    private Gson gson = new Gson();

    public KinNativeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        init("vNiX", "TEST");
    }

    @Override
    public String getName() {
        return "KinNativeModule";
    }
    
    private Environment getEnvironment(String environment) {
        Environment environ;
        switch (environment) {
            case "production":
            case "PRODUCTION":
                environ = Environment.PRODUCTION;
                break;
            default:
                environ = Environment.TEST;
                break;
        }
        return environ;
    }
    
    private void init (String appId, String environment) {
        getClient(appId, environment);
        getOrCreateAccount();
        
    }

    @ReactMethod
    public void sayHi(Callback cb) {
        cb.invoke("Callback : Greetings from Java: I know, i know");
    }

    @ReactMethod
    public void getClient(String appId, String environment, Callback cb) {
        Environment env = getEnvironment(environment);
        kinClient = new KinClient(getReactApplicationContext(), env, appId);
        cb.invoke(null, gson.toJson(kinClient));
    }
    
    private KinClient getClient(String appId, String environment) {
        Environment env = getEnvironment(environment);
        kinClient = new KinClient(getReactApplicationContext(), env, appId);
        return kinClient;
    }

    @ReactMethod
    public void createAccount(Callback cb) {
        try {
            if (!kinClient.hasAccount()) {
                kinAccount = kinClient.addAccount();
            }
        } catch (CreateAccountException ex) {
            ex.printStackTrace();
        }
        cb.invoke(null, gson.toJson(kinAccount));
    }
    
    private KinAccount getOrCreateAccount() {
        if (kinAccount != null) return kinAccount;
        else if (kinClient.hasAccount()) {
            kinAccount = kinClient.getAccount(0);
            return kinAccount;
        }
        else {
            try {
                kinAccount = kinClient.addAccount();
            } catch (Exception ex) {
                System.out.println(ex);
            }
            return kinAccount;
        }
    }

    @ReactMethod
    public void getAccount(Callback cb) {
        if (kinAccount != null) cb.invoke(null, gson.toJson(kinAccount));
        else if (kinClient.hasAccount()) {
            kinAccount = kinClient.getAccount(0);
            cb.invoke(null, gson.toJson(kinAccount));
        }
        else cb.invoke(gson.toJson(new Exception("Cannot find an associated kin account for the kin client")));
    }

    @ReactMethod
    public void deleteAccount(int index, Callback cb) throws Exception {
        if (!kinClient.hasAccount()) cb.invoke(gson.toJson(new Exception("Kin client does not have any account")));
        kinClient.deleteAccount(index);
        cb.invoke(null, index);
    }

    @ReactMethod
    public void deleteAccount(Callback cb) throws Exception {
        deleteAccount(0, cb);
    }

    @ReactMethod
    public void getPublicAddress(Callback cb) {
        if (publicAddress != null) cb.invoke(null, publicAddress);
        else {
            publicAddress = kinAccount.getPublicAddress();
            cb.invoke(null, publicAddress);
        }
    }

    @ReactMethod
    public void getStatus(final Callback cb) {
        Request<Integer> statusRequest = kinAccount.getStatus();
        statusRequest.run(new ResultCallback<Integer>() {
            @Override
            public void onResult(Integer result) {
                switch (result) {
                    case AccountStatus.CREATED:
                        cb.invoke(null, gson.toJson(new Status("Kin account has been created", 200)));
                        break;
                    case AccountStatus.NOT_CREATED:
                        Error err = new Error();
                        err.setCode(ErrorCode.ERR_DATA);
                        err.setMsg("Kin account has not been created");
                        cb.invoke(gson.toJson(err));
                        break;
                }
            }

            @Override
            public void onError(Exception e) {
                cb.invoke(gson.toJson(e));
            }
        });
    }

    @ReactMethod
    public void getBalance(final Callback cb) {
        Request<Balance> balanceRequest = kinAccount.getBalance();
        balanceRequest.run(new ResultCallback<Balance>() {

            @Override
            public void onResult(Balance result) {
                cb.invoke(null, gson.toJson(result));
            }

            @Override
            public void onError(Exception e) {
                cb.invoke(gson.toJson(e));
            }
        });
    }

    @ReactMethod
    public void buildTransaction(String recipientAddress, BigDecimal amount, final Callback cb) {
        try {
            buildTransaction(recipientAddress, amount, getCurrentMinimumFee(), cb);
        } catch (Exception e) {
            cb.invoke(gson.toJson(e));
        }
    }

    @ReactMethod
    public void buildTransaction(String recipientAddress, BigDecimal amount, int fee, final Callback cb) {
        buildTransaction(recipientAddress, amount, fee, null, cb);
    }

    @ReactMethod
    public void buildTransaction(String recipientAddress, BigDecimal amount, int fee, String memo, final Callback cb) {
        Request<Transaction> transactionRequest = kinAccount.buildTransaction(recipientAddress, amount, fee, memo);
        transactionRequest.run(new ResultCallback<Transaction>() {
            @Override
            public void onResult(Transaction transaction) {
                Request<TransactionId> sendTransactionRequest = kinAccount.sendTransaction(transaction);
                sendTransactionRequest.run(new ResultCallback<TransactionId>() {
                    @Override
                    public void onResult(TransactionId transactionId) {
                        cb.invoke(null, gson.toJson(transactionId));
                    }

                    @Override
                    public void onError(Exception e) {
                        cb.invoke(gson.toJson(e));
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                cb.invoke(gson.toJson(e));
            }
        });
    }

    @ReactMethod
    public void buildWhitelistedTransaction(String recipientAddress, BigDecimal amount, int fee, final Callback cb) {

    }

    @ReactMethod
    public void addPaymentListener(final Callback cb) {
        ListenerRegistration listenerRegistration = kinAccount.addPaymentListener(new EventListener<PaymentInfo>() {
            @Override
            public void onEvent(PaymentInfo payment) {
                cb.invoke(gson.toJson(payment));
            }
        });
    }

    @ReactMethod
    public void addBalanceListener(final Callback cb) {
        ListenerRegistration listenerRegistration = kinAccount.addBalanceListener(new EventListener<Balance>() {
            @Override
            public void onEvent(Balance balance) {
                cb.invoke(gson.toJson(balance));
            }
        });
    }

    @ReactMethod
    public void addAccountCreationListener(final Callback cb) {
        ListenerRegistration listenerRegistration = kinAccount.addAccountCreationListener(new EventListener<Void>() {
            @Override
            public void onEvent(Void result) {
                cb.invoke(gson.toJson(result));
            }
        });
    }

    @ReactMethod
    public void getCurrentMinimumFee(Callback cb){
        try{
            int fee = (int) Math.ceil(kinClient.getMinimumFeeSync());
            cb.invoke(null, fee);
        } catch(Exception ex) {
            cb.invoke(gson.toJson(ex));
        }
    }
    private int getCurrentMinimumFee() throws Exception{
        return (int) Math.ceil(kinClient.getMinimumFeeSync());
    }
}