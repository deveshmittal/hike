package com.bsb.hike.offline;

import java.util.ArrayList;

public interface FinishScanListener {

	void onFinishScan(ArrayList<ClientScanResult> result);

}
