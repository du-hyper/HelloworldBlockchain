package com.xingkaichun.helloworldblockchain.core.impl;

import com.xingkaichun.helloworldblockchain.core.VirtualMachine;
import com.xingkaichun.helloworldblockchain.core.model.script.BooleanEnum;
import com.xingkaichun.helloworldblockchain.core.model.script.OperationCodeEnum;
import com.xingkaichun.helloworldblockchain.core.model.script.Script;
import com.xingkaichun.helloworldblockchain.core.model.script.ScriptExecuteResult;
import com.xingkaichun.helloworldblockchain.core.model.transaction.Transaction;
import com.xingkaichun.helloworldblockchain.core.tools.TransactionTool;
import com.xingkaichun.helloworldblockchain.crypto.AccountUtil;
import com.xingkaichun.helloworldblockchain.crypto.HexUtil;
import com.xingkaichun.helloworldblockchain.util.StringUtil;

import java.util.Arrays;

/**
 * 基于栈的虚拟机
 *
 * @author 邢开春 409060350@qq.com
 */
public class StackBasedVirtualMachine extends VirtualMachine {

    /**
     * 执行脚本
     */
    public ScriptExecuteResult executeScript(Transaction transactionEnvironment, Script script) throws RuntimeException {
        ScriptExecuteResult stack = new ScriptExecuteResult();

        for(int i=0;i<script.size();i++){
            String command = script.get(i);
            byte[] byteCommand = HexUtil.hexStringToBytes(command);
            if(Arrays.equals(OperationCodeEnum.OP_DUP.getCode(),byteCommand)){
                if(stack.size()<1){
                    throw new RuntimeException("指令运行异常");
                }
                stack.push(stack.peek());
            }else if(Arrays.equals(OperationCodeEnum.OP_HASH160.getCode(),byteCommand)){
                if(stack.size()<1){
                    throw new RuntimeException("指令运行异常");
                }
                String publicKey = stack.pop();
                String publicKeyHash = AccountUtil.publicKeyHashFromPublicKey(publicKey);
                stack.push(publicKeyHash);
            }else if(Arrays.equals(OperationCodeEnum.OP_EQUALVERIFY.getCode(),byteCommand)){
                if(stack.size()<2){
                    throw new RuntimeException("指令运行异常");
                }
                if(!StringUtil.isEquals(stack.pop(),stack.pop())){
                    throw new RuntimeException("脚本执行失败");
                }
            }else if(Arrays.equals(OperationCodeEnum.OP_CHECKSIG.getCode(),byteCommand)){
                if(stack.size()<2){
                    throw new RuntimeException("指令运行异常");
                }
                String publicKey = stack.pop();
                String signature = stack.pop();
                String message = TransactionTool.signatureHashAll(transactionEnvironment);
                boolean verifySignatureSuccess = AccountUtil.verifySignature(publicKey,message,signature);
                if(!verifySignatureSuccess){
                    throw new RuntimeException("脚本执行失败");
                }
                stack.push(HexUtil.bytesToHexString(BooleanEnum.TRUE.getCode()));
            }else if(Arrays.equals(OperationCodeEnum.OP_PUSHDATA.getCode(),byteCommand)){
                if(script.size()<=i){
                    throw new RuntimeException("指令运行异常");
                }
                stack.push(script.get(++i));
            }else {
                throw new RuntimeException("不能识别的指令");
            }
        }
        return stack;
    }

}
