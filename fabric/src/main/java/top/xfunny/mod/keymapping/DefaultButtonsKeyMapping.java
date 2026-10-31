package top.xfunny.mod.keymapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DefaultButtonsKeyMapping {
    final private Map<String, ButtonArea> buttonMap = new HashMap<String, ButtonArea>();

    public void registerButton(String buttonName, float[] location, float[] dimension) {
        // 检查是否已存在该名称的按钮
        if (buttonMap.containsKey(buttonName)) {
            ButtonArea existingArea = buttonMap.get(buttonName);
            // 如果位置location发生了变化
            if (!Arrays.equals(existingArea.location, location)) {
                //移除旧项
                buttonMap.remove(buttonName);
                //存入新项
                buttonMap.put(buttonName, new ButtonArea(location, dimension));
            }
        } else {
            // 如果不存在，直接添加
            buttonMap.put(buttonName, new ButtonArea(location, dimension));
        }
    }

    public void removeButton(String buttonName) {
        buttonMap.remove(buttonName);
    }// 暂时用不到

    /** Clears per-frame hit areas before the in-world touchscreen is laid out again. */
    public void clearButtons() {
        buttonMap.clear();
    }

    public String mapping(double x, double hitY) {
        for (Map.Entry<String, ButtonArea> entry : buttonMap.entrySet()) {
            ButtonArea area = entry.getValue();
            boolean hit = x < -(area.location[0] - 0.5) && x > -(area.location[0] - 0.5) - area.dimension[0]
                    && hitY > area.location[1] && hitY < area.location[1] + area.dimension[1];
            if (hit) {
                return entry.getKey();
            }
        }
        return "null";
    }

    public int getButtonCount() {
        return buttonMap.size();
    }// 暂时用不到

    private static class ButtonArea {
        float[] location;
        float[] dimension;

        ButtonArea(float[] location, float[] dimension) {
            this.location = location;
            this.dimension = dimension;
        }

    }

}
