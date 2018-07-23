package com.zhuoan.biz.robot;

import com.zhuoan.biz.core.qzmj.MaJiangCore;
import com.zhuoan.constant.QZMJConstant;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MaJiangAI {

    public static void main(String[] args) {

        // 0,1,2,3,4,5,6,7,8
        // 9,10,11,12,13,14,15,16,17
        // 18,19,20,21,22,23,24,25,26
        // 27,28,29,30,31,32,33
        int[] list = {3, 19, 17, 27, 32, 1, 1, 14, 14, 14, 5, 0, 24, 4, 10, 13, 21};

        JSONObject special = new JSONObject();
        special.put("mj_count", 34);
        int jin = -1;
        int pai = getRobotChupai(list, special, jin);
        System.out.println("出牌：" + pai);
    }

    /**
     * 获取机器人出牌
     *
     * @param list
     * @param special
     * @param jin
     * @return
     */
    static int getRobotChupai(int[] list, JSONObject special, int jin) {

        int listSize = list.length;
        int mjCount = special.getInt("mj_count");
        int[] arr = new int[mjCount];

        for (int j = 0; j < listSize; j++) {

            arr[list[j]]++;
        }
        //假设所有牌都需要2个混子补缺
        int ret_needhun = 32;
        int ret_nexus = 0;
        int ret_pai = list[0];
        List<Integer> ret_tinglist = new ArrayList<Integer>();
        boolean has_single = false;
        for (int k = 0; k < list.length; k++) {

            if (list[k] == jin) {
                continue;
            }
            arr[list[k]]--;

            int[] new_list = Arrays.copyOf(list, list.length);
            new_list[k] = -1;
            special.put("pai", new_list);
            // 判断是否可以听牌
            List<Integer> tingList = canTingPai(arr, jin, special);
            int tingSize = tingList.size();
            if (tingSize > 0) {
                //听牌数比对，也可以按其他方式比对，比如所听的牌接下来的剩余牌
                if (ret_tinglist.size() < tingSize) {
                    ret_tinglist = tingList;
                    ret_needhun = 1;
                    ret_pai = list[k];
                }

            } else if (ret_tinglist.size() == 0) {
                int needhun = getNeedhunForHu(arr, jin, special);
                int nexus = has_nexus(list[k], arr);
                //如果是单一的手牌优先考虑
                if (nexus == 0) {
                    //如果之前没有过单一的牌
                    if (!has_single) {
                        ret_needhun = needhun;
                        ret_pai = list[k];
                    }
                    has_single = true;
                    //风牌优先打
                    if (list[k] > 26) {
                        ret_needhun = needhun;
                        ret_pai = list[k];
                    }
                    //边牌优先打
                    if ((list[k] % 9 < 1 || list[k] % 9 > 7) && ret_pai <= 26) {
                        ret_needhun = needhun;
                        ret_pai = list[k];
                    }
                } else if (!has_single) {
                    //如果不是单一的手牌，且之前也没有过单一的牌
                    //判断此张牌需要的混牌数
                    if (needhun < ret_needhun) {
                        ret_needhun = needhun;
                        ret_pai = list[k];
                    } else if (needhun == ret_needhun) {
                        if (nexus < ret_nexus) {
                            ret_nexus = nexus;
                            ret_pai = list[k];
                        } else if (nexus == ret_nexus) {
                            //风牌优先打
                            if (list[k] > 26) {
                                ret_pai = list[k];
                            }
                            //边牌优先打
                            if ((list[k] % 9 < 1 || list[k] % 9 > 7) && ret_pai <= 26) {
                                ret_pai = list[k];
                            }
                        }
                    }

                }
            }
            arr[list[k]]++;

        }
        return ret_pai;
    }

    /**
     * 获取听牌列表
     *
     * @param arr
     * @param jin
     * @param special
     * @return
     */
    static List<Integer> canTingPai(int[] arr, int jin, JSONObject special) {

        List<Integer> myPai = getMaJiangListByIndex(arr, special);
        List<Integer> tingList = MaJiangCore.isTingPai(myPai, jin);
        if (tingList.size() > 0) {
            System.out.println(JSONArray.fromObject(myPai));
            System.out.println(JSONArray.fromObject(tingList));
        }
        return getMaJiangIndex(tingList, special);
    }


    /**
     * 根据下标获取麻将牌
     *
     * @param arr
     * @param special
     * @return
     */
    static List<Integer> getMaJiangListByIndex(int[] arr, JSONObject special) {

        List<Integer> myPai = new ArrayList<Integer>();
        JSONArray pais = special.getJSONArray("pai");
        for (int i = 0; i < pais.size(); i++) {
            int index = pais.getInt(i);
            if (index >= 0) {
                myPai.add(QZMJConstant.ALL_CAN_HU_PAI[index]);
            }
        }
        return myPai;
    }


    /**
     * 获取麻将下标
     *
     * @param pais
     * @param special
     * @return
     */
    static List<Integer> getMaJiangIndex(List<Integer> pais, JSONObject special) {

        List<Integer> paiIndex = new ArrayList<Integer>();
        int[] paiArray = QZMJConstant.ALL_CAN_HU_PAI;
        for (int i : pais) {
            for (int j = 0; j < paiArray.length; j++) {
                if (i == paiArray[j]) {
                    paiIndex.add(j);
                }
            }
        }
        return paiIndex;
    }


    /**
     * 返回单牌和手牌有关系的个数
     *
     * @param i
     * @param arr
     * @return
     */
    static int has_nexus(int i, int[] arr) {

        if (i > 26) {
            return arr[i];
        } else if (i % 9 == 8) {
            return arr[i] + arr[i - 1] + arr[i - 2];
        } else if (i % 9 == 7) {
            return arr[i] + arr[i - 1] + arr[i - 2] + arr[i + 1];
        } else if (i % 9 == 0) {
            return arr[i] + arr[i + 1] + arr[i + 2];
        } else if (i % 9 == 1) {
            return arr[i] + arr[i + 1] + arr[i + 2] + arr[i - 1];
        } else {
            return arr[i] + arr[i + 1] + arr[i + 2] + arr[i - 1] + arr[i - 2];
        }
    }

    static JSONObject fmin_data(JSONObject data1, JSONObject data2) {
        return data1.getInt("needhun") > data2.getInt("needhun") ? data2 : data1;
    }

    static JSONObject del_list(int[] old_arr, int i, int j, JSONObject data) {

        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        for (int k = 0; k < 3; k++) {
            if (arr[i + k] > 0) {
                arr[i + k]--;
            } else {
                int needhun = data.getInt("needhun");
                needhun++;
                data.put("needhun", needhun);
            }
        }
        return dfs(arr, i, j, data);
    }

    static JSONObject del_same(int[] old_arr, int i, int j, JSONObject data) {

        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        arr[i] %= 3;
        switch (arr[i]) {
            case 0: {
                break;
            }
            case 1: {
                if (data.getBoolean("hasjiang")) {
                    int needhun = data.getInt("needhun");
                    needhun += 2;
                    data.put("needhun", needhun);
                } else {
                    int needhun = data.getInt("needhun");
                    needhun++;
                    data.put("needhun", needhun);
                    data.put("hasjiang", true);
                }
                break;
            }
            case 2: {
                if (data.getBoolean("hasjiang")) {
                    int needhun = data.getInt("needhun");
                    needhun += 1;
                    data.put("needhun", needhun);
                } else {
                    data.put("hasjiang", true);
                }
                break;
            }
            default:
                break;
        }
        arr[i] = 0;
        return dfs(arr, i + 1, j, data);
    }

    static JSONObject dfs(int[] arr, int i, int j, JSONObject data) {
        if (i > j) {
            if (!data.getBoolean("hasjiang")) {
                int needhun = data.getInt("needhun");
                needhun += 2;
                data.put("needhun", needhun);
            }
            data.put("arr", arr);
            return data;
        }
        //8 9特殊情况，此时应该补个7
        if (i % 9 == 6 && i < 27 && arr[i + 1] % 3 == 1 && arr[i + 2] % 3 == 1) {
            return del_list(arr, i, j, data);
        } else if (arr[i] == 0) {
            return dfs(arr, i + 1, j, data);
        } else if (i % 9 < 7 && i < 27 && (arr[i + 1] > 0 || arr[i + 2] > 0)) {

            int needhun = data.getInt("needhun");
            boolean hasjiang = data.getBoolean("hasjiang");

            JSONObject d = new JSONObject();
            d.put("needhun", needhun);
            d.put("hasjiang", hasjiang);

            JSONObject tmp1 = del_list(arr, i, j, d);
            JSONObject tmp2 = del_same(arr, i, j, d);

            return fmin_data(tmp1, tmp2);
        } else {
            return del_same(arr, i, j, data);
        }
    }


    static int getneedhun(int[] old_arr, int type, boolean hasjiang) {

        JSONObject data = new JSONObject();
        data.put("needhun", 0);
        data.put("hasjiang", hasjiang);
        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        int i = 0, j = 0;
        switch (type) {
            case 0: {
                i = 0;
                j = 8;
                break;
            }
            case 1: {
                i = 9;
                j = 17;
                break;
            }
            case 2: {
                i = 18;
                j = 26;
                break;
            }
            case 3: {
                i = 27;
                j = 33;
                break;
            }
            default:
                break;
        }
        data = dfs(arr, i, j, data);
        return data.getInt("needhun");
    }

    /**
     * 获取胡牌需要的张数
     *
     * @param old_arr
     * @param Hun
     * @param special
     * @return
     */
    static int getNeedhunForHu(int[] old_arr, int Hun, JSONObject special) {

        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        int HunCount = 0;
        if (Hun >= 0) {
            HunCount = arr[Hun];
            arr[Hun] = 0;
        }
        int min_needhun = 32;

        for (int i = 0; i < 4; i++) {
            int needhun = 0;
            for (int j = 0; j < 4; j++) {
                needhun += getneedhun(arr, j, j != i);
            }
            if (needhun < min_needhun) {
                min_needhun = needhun;
            }
        }
        return min_needhun - HunCount;
    }

}
