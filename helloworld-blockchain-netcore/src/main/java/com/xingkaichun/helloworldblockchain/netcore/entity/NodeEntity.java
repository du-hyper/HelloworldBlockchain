package com.xingkaichun.helloworldblockchain.netcore.entity;

/**
 *
 * @author 邢开春 409060350@qq.com
 */
public class NodeEntity{
    private String ip;
    private Long blockchainHeight;




    //region get set
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getBlockchainHeight() {
        return blockchainHeight;
    }

    public void setBlockchainHeight(Long blockchainHeight) {
        this.blockchainHeight = blockchainHeight;
    }

    //endregion
}
