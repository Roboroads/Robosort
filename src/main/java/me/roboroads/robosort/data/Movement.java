package me.roboroads.robosort.data;

public class Movement {
    public final int furniId;
    public final String variableId;
    public final int value;

    public Movement(int furniId, String variableId, int value) {
        this.furniId = furniId;
        this.variableId = variableId;
        this.value = value;
    }
}
