package io.netbird.client.ui.egress;

public class EgressGroup {
    public final String id;
    public final String name;
    public final int peersCount;

    public EgressGroup(String id, String name, int peersCount) {
        this.id = id;
        this.name = name;
        this.peersCount = peersCount;
    }
}
