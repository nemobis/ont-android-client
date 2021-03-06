/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
 * Copyright 2013-2015 alladin-IT GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.specure.android.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import at.specure.android.screens.main.MainActivity;
import at.specure.android.api.calls.CheckTestResultDetailTask;
import at.specure.android.configs.ConfigHelper;
import at.specure.android.screens.result.ResultDetailType;
import at.specure.android.util.EndTaskListener;
import at.specure.client.v2.task.result.QoSServerResult.DetailType;
import at.specure.client.v2.task.result.QoSServerResultCollection;
import at.specure.client.v2.task.result.QoSTestResultEnum;

import com.google.gson.JsonArray;
import com.specure.opennettest.R;

/**
 * 
 * @author lb
 *
 */
public class ResultQoSDetailView extends ScrollView implements EndTaskListener, OnItemClickListener, OnClickListener {
	
	private final MainActivity activity;
	
    private final String uid;
	
	private View view;
	
	private EndTaskListener resultFetchEndTaskListener;
	
    private CheckTestResultDetailTask testResultDetailTask;;
    
    private JsonArray testResult;
    
    private QoSServerResultCollection results;

	public ResultQoSDetailView(Context context, MainActivity activity, String uid, JsonArray jsonArray) {
		this(context, null, activity, uid, jsonArray);
	}
	
	public ResultQoSDetailView(Context context, AttributeSet attrs, MainActivity activity, String uid, JsonArray jsonArray) {
		super(context, attrs);
		setFillViewport(true);
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.activity = activity;
		this.uid = uid;
		this.view = createView(layoutInflater);
		this.testResult = jsonArray;
	}
    
    public void initialize(EndTaskListener resultFetchEndTaskListener) {        
    	this.resultFetchEndTaskListener = resultFetchEndTaskListener;
    	
        if ((testResultDetailTask == null || testResultDetailTask != null || testResultDetailTask.isCancelled()) && uid != null)
        {
        	if (this.testResult!=null) {
        		taskEnded(this.testResult);
        	}
        	else {
            	System.out.println("initializing ResultDetailsView");
            	
                testResultDetailTask = new CheckTestResultDetailTask(activity, ResultDetailType.QUALITY_OF_SERVICE_TEST);
                
                testResultDetailTask.setEndTaskListener(this);
                testResultDetailTask.execute(uid);        		
        	}
        }
    }
    
    /**
     * 
     * @param inflater
     * @return
     */
	public View createView(final LayoutInflater inflater)
    {        
    	final View view = inflater.inflate(R.layout.result_qos_details, this);
        
        return view;
    }
	
	public View getView() {
		return view;
	}
	
	/*
	 * (non-Javadoc)
	 * @see at.specure.android.util.EndTaskListener#taskEnded(org.json.JSONArray)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void taskEnded(JsonArray result) {
		System.out.println("ResultQoSDetail taskEnded");
		this.testResult = result;
		
		if (resultFetchEndTaskListener != null) {
			resultFetchEndTaskListener.taskEnded(result);
		}
		
		ProgressBar resultProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		TextView resultTextView = (TextView) view.findViewById(R.id.info_text);
		
		try {
			results = new QoSServerResultCollection(result);
			
			View successList = view.findViewById(R.id.qos_success_list);

			List<HashMap<String, String>> itemList = new ArrayList<HashMap<String,String>>();
			int index = 0;
			for (QoSTestResultEnum type : QoSTestResultEnum.values()) {
				if (results.getQoSStatistics().getTestCounter(type) > 0) {
					HashMap<String, String> listItem = new HashMap<String, String>();
					listItem.put("name", ConfigHelper.getCachedQoSNameByTestType(type, activity));
					listItem.put("type_name", type.toString());
					listItem.put("index", String.valueOf(index++));
					itemList.add(listItem);
				}
			}
			
            ListAdapter valueList = new SimpleAdapter(activity, itemList, R.layout.qos_category_list_item, new String[] {
            "name"}, new int[] { R.id.name});

    		resultProgressBar.setVisibility(View.GONE);
    		
            if (valueList.getCount() > 0) {
            	
	            //in case the view will change again:
	            if (ListView.class.isAssignableFrom(successList.getClass())) {
		            ((ListView) successList).setAdapter(valueList);
		            ((ListView) successList).setOnItemClickListener(this);
	            }
	            else {
	            	ViewGroup vgList = (ViewGroup) successList;
	            	for (int i = 0; i < valueList.getCount(); i++) {
	            		View v = valueList.getView(i, null, null);
	            		
	            		QoSTestResultEnum key = QoSTestResultEnum.valueOf(((HashMap<String, String>)valueList.getItem(i)).get("type_name"));
	            		if (results.getQoSStatistics().getFailureCounter(key) > 0) {
		            		ImageView img = (ImageView) v.findViewById(R.id.status);
		            		img.setImageResource(R.drawable.traffic_lights_red);
	            		}
	            		
	            		TextView status = (TextView) v.findViewById(R.id.qos_type_status);
	            		status.setText((results.getQoSStatistics().getTestCounter(key) - results.getQoSStatistics().getFailedTestsCounter(key)) 
	            				+ "/" + results.getQoSStatistics().getTestCounter(key));

	            		v.setOnClickListener(this);
	            		v.setTag(valueList.getItem(i));
	            		vgList.addView(v);
	            	}
	            }
                
        		successList.invalidate();
        		
        		resultTextView.setVisibility(View.GONE);
        		successList.setVisibility(View.VISIBLE);	
            }
            else {
            	resultTextView.setText(R.string.result_qos_error_no_data_available);
            }

		} catch (Throwable t) {
			resultTextView.setText(R.string.result_qos_error_no_data_available);
			resultProgressBar.setVisibility(View.GONE);
			t.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		ListView listView = (ListView) adapter;
		HashMap<String, String> item = (HashMap<String, String>) listView.getAdapter().getItem(position);
		if (listView.getId() == R.id.qos_success_list) {
			activity.showExpandedResultDetail(results, DetailType.OK, Integer.valueOf(item.get("index")));
		}
		else if (listView.getId() == R.id.qos_error_list) {
			activity.showExpandedResultDetail(results, DetailType.FAIL, Integer.valueOf(item.get("index")));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onClick(View v) {
		try {
			HashMap<String, String> item = (HashMap<String, String>) v.getTag();
			if (((View) v.getParent()).getId() == R.id.qos_success_list) {
				activity.showExpandedResultDetail(results, DetailType.OK, Integer.valueOf(item.get("index")));
			}
			else if (((View) v.getParent()).getId() == R.id.qos_error_list) {
				activity.showExpandedResultDetail(results, DetailType.FAIL, Integer.valueOf(item.get("index")));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
