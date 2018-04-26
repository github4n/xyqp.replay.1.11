package com.zhuoan.biz.core.nn;

/**
 * 扑克牌
 */
public class Packer {

    private Num num;
    private Color color;

    public Packer(String num){
        this.num=Num.valueOf(num);
    }

    public Packer(Num num,Color color){
        this.num=num;
        this.color=color;
    }
    public Packer(){
    }
    public Num getNum() {
        return num;
    }
    public void setNum(Num num) {
        this.num = num;
    }
    public Color getColor() {
        return color;
    }
    public void setColor(Color color) {
        this.color = color;
    }


    /**
     * 牌比大小
     * @param p
     * @return
     */
    public int compare(Packer p){
        int compare=this.num.getNum()==p.num.getNum()?0:this.num.getNum()>p.num.getNum()?1:-1;
        if(compare!=0){
            return compare;
        }else{
            //牌点相同比花色
            compare=this.getColor().getColor()==p.getColor().getColor()?0:this.getColor().getColor()>p.getColor().getColor()?1:-1;
        }
        return compare;
    }


    /**
     * 牌只比点数
     * @param p
     * @return
     */
    public int compareNum(Packer p){
        int t=this.getNum().getNum();
        int p1=p.getNum().getNum();

        if(t==p1)return 0;
        return t>p1?1:-1;
    }


    /**
     * 排序
     * @param ps
     * @return
     */
    public static Packer[] sort(Packer[] ps){
        for(int i=0;i<ps.length;i++){
            int k=i;
            for(int j=i-1;j>=0;j--){
                if(ps[k]!=null&&ps[k].compare(ps[j])<0){
                    exchange(ps,k,j);
                    k--;
                }else{
                    break;
                }
            }
        }
        return ps;
    }


    /**
     * 交换位置
     * @param ps
     * @param i
     * @param j
     */
    private static void exchange(Packer[] ps ,int i,int j){
        Packer p=ps[i];
        ps[i]=ps[j];
        ps[j]=p;
    }
	
}
