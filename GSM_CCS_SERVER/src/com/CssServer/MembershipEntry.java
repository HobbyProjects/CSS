package com.CssServer;

import com.CssServer.GroupNameEntry;

public class MembershipEntry {
	public MembershipEntry(String _userid, GroupNameEntry _membership) {
		userid = _userid;
		membership = _membership;
	}
	public String userid;
	public GroupNameEntry membership;
}
