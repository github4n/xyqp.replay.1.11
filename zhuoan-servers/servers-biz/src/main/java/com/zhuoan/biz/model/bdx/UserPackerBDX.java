package com.zhuoan.biz.model.bdx;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 14:54 2018/4/26
 * @Modified By:
 **/
public class UserPackerBDX {
    private int status;
    private double value;
    private double score;
    private int[] pai;
    private int isWin;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int[] getPai() {
        return pai;
    }

    public void setPai(int[] pai) {
        this.pai = pai;
    }

    public int getIsWin() {
        return isWin;
    }

    public void setIsWin(int isWin) {
        this.isWin = isWin;
    }

    public void initUserPackerBDX(){

    }
}
