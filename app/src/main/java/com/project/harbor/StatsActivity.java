package com.project.harbor;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;

public class StatsActivity extends ListActivity {
    private ArrayAdapter<String> adapter;
    private Comparator<String> comparator;
    private int sort;

    private final int NAME_SORT=1, DATE_SORT=2, LEVEL_SORT=3, PARKED_SORT=4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> stats = (getIntent().getStringArrayListExtra("STATS"));
        stats = (stats != null) ? stats : new ArrayList<String>();
        if(savedInstanceState != null) sort = savedInstanceState.getInt("SORT", NAME_SORT);

        String[] data = new String[stats.size()];
        StringBuilder sb = new StringBuilder();
        int ind = 0;
        for(String str : stats) {
            String[] infos = str.split(",");
            int j;
            for(j = 0; j < infos.length - 1; j++) {
                 sb.append(infos[j]);
                 sb.append("  |  ");
            }
            sb.append(infos[j]);
            data[ind++] = sb.toString();
            sb.delete(0, sb.length());
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1,
                data);

        comparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String []d1 = o1.split("[|]");
                String []d2 = o2.split("[|]");

                // default sort, this might not be happen
                if(d1.length != 5 || d2.length != 5) return o1.compareTo(o2);

                switch (sort) {
                    case NAME_SORT : // sort by username
                        return d1[0].compareTo(d2[0]);
                    case DATE_SORT : // sort by date
                        return d2[1].compareTo(d1[1]);
                    case LEVEL_SORT : // sort by level
                        return d1[2].compareTo(d2[2]);
                    case PARKED_SORT : // sort by number parked
                        return Integer.parseInt(d2[3].trim()) - Integer.parseInt(d1[3].trim());
                    default: // sort by time
                        double diff = Double.parseDouble(d2[4].trim())
                                - Double.parseDouble(d1[4].trim());
                        if(diff < 0) return -1;
                        if (diff == 0) return 0;
                        return 1;
                }
            }
        };

        adapter.sort(comparator);
        setListAdapter(adapter);

        //Add header for the list
        ListView lv = getListView();
        LayoutInflater inflater = getLayoutInflater();
        View header = inflater.inflate(R.layout.header, lv, false);
        lv.addHeaderView(header, null, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stats, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.sort_name :
                sort = NAME_SORT;
                break;
            case R.id.sort_date :
                sort = DATE_SORT;
                break;
            case R.id.sort_level :
                sort = LEVEL_SORT;
                break;
            case R.id.sort_parked :
                sort = PARKED_SORT;
                break;
            default :
                sort = -1; // default, time sort
                break;
        }
        adapter.sort(comparator);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("SORT", sort);
    }
}
