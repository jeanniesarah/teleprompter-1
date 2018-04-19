package com.ctp.theteleprompter;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ctp.theteleprompter.adapters.DocGridAdapter;
import com.ctp.theteleprompter.data.SharedPreferenceUtils;
import com.ctp.theteleprompter.data.TeleContract;
import com.ctp.theteleprompter.model.Doc;
import com.ctp.theteleprompter.services.DocService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
            implements DocGridAdapter.DocGridAdapterCallbacks,
                LoaderManager.LoaderCallbacks<Cursor>{

    private static final int DOC_LOADER_ID = 10001;
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.main_doc_recycler_view)
    RecyclerView docGridView;

    @BindView(R.id.main_refresh_layout)
    SwipeRefreshLayout progressBar;

    @BindView(R.id.ad_view)
    AdView mAdView;

    @BindColor(R.color.colorDanger)
    int colorDanger;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;


    private DocGridAdapter adapter;

//    private IntentFilter intentFilter;
//    private CardServiceBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_drawer);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        initializeWidgets();



    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if(id==DOC_LOADER_ID){
            progressBar.setRefreshing(true);
            return new CursorLoader(this, TeleContract.TeleEntry.TELE_CONTENT_URI,null,null,null,
                    TeleContract.TeleEntry.COLUMN_PRIORITY+" DESC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        progressBar.setRefreshing(false);
        if(data==null){
            return;
        }

        adapter.swapCursor(data);
        Log.d(TAG,"Calles Swap cursor");
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
               drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this,LoginActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private int numberOfColumns() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // You can change this divider to adjust the size of the poster
        int widthDivider = 500;
        int width = displayMetrics.widthPixels;
        int nColumns = width / widthDivider;
        if (nColumns < 2) return 2;
        return nColumns;
    }

    @Override
    public void onDocClicked(Doc doc) {

        Intent intent = new Intent(this,DocEditActivity.class);
        intent.putExtra(DocEditActivity.EXTRA_PARCEL_KEY,doc);
        startActivity(intent);

    }

    private void initializeWidgets(){

        /*  Initialize floating add butto   */

        FloatingActionButton fab = findViewById(R.id.fab);

        /*  Start new DocEdit Activity Intent or fragment */
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent intent = new Intent(MainActivity.this,DocEditActivity.class);
               Doc doc = new Doc();
               doc.setTitle("");
               doc.setText("");
               doc.setNew(true);
               doc.setUserId(SharedPreferenceUtils.getPrefUserId(MainActivity.this));
               intent.putExtra(DocEditActivity.EXTRA_PARCEL_KEY,doc);
               startActivity(intent);

//               TODO: Start new fragment for tablets
            }
        });


        StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(numberOfColumns(),StaggeredGridLayoutManager.VERTICAL);

        docGridView.setLayoutManager(manager);
        adapter = new DocGridAdapter(this,null);

        progressBar.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getSupportLoaderManager().restartLoader(DOC_LOADER_ID,null,MainActivity.this);
            }
        });


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN
                | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int source = viewHolder.getAdapterPosition();
                int destination = target.getAdapterPosition();
                adapter.move(source,destination);
//

                return true;
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        List<Doc> orderedDocs = adapter.getDocList();

                        for(int i=0;i<orderedDocs.size();i++){

                            Doc d = orderedDocs.get(i);
                            Log.d(TAG,"Set Priority of "+d.getTitle()+" from "+d.getPriority() +" to "+(orderedDocs.size()-i));
                            int newPriority = orderedDocs.size()-i;
                            d.setPriority(newPriority);

                            DocService.updateDoc(MainActivity.this,d);

                        }

                    }
                },1000);


            }



            // Called when a user swipes left or right on a ViewHolder
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Here is where you'll implement swipe to delete

                // COMPLETED (1) Construct the URI for the item to delete
                //[Hint] Use getTag (from the adapter code) to get the id of the swiped item
                // Retrieve the id of the task to delete
                int position = viewHolder.getAdapterPosition();


                Doc doc = adapter.getDocAtPosition(position);

                DocService.deleteDoc(MainActivity.this,doc);
                adapter.deletePosition(position);
                // Build appropriate uri with String row id appended
//                String stringId = Integer.toString(id);
//                Uri uri = TaskContract.TaskEntry.CONTENT_URI;
//                uri = uri.buildUpon().appendPath(stringId).build();
//
//                // COMPLETED (2) Delete a single row of data using a ContentResolver
//                getContentResolver().delete(uri, null, null);
//
//                // COMPLETED (3) Restart the loader to re-query for all tasks after a deletion
//                getSupportLoaderManager().restartLoader(TASK_LOADER_ID, null, MainActivity.this);
            }
        }).attachToRecyclerView(docGridView);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id){
                    case R.id.nav_settings:
                        Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
                        startActivity(intent);
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.nav_logout:
                        FirebaseAuth mAuth = FirebaseAuth.getInstance();
                        mAuth.signOut();
                        Intent theIntent = new Intent(MainActivity.this,LoginActivity.class);
                        startActivity(theIntent);
                        break;
                }
                return false;
            }
        });


        docGridView.setAdapter(adapter);
        MobileAds.initialize(this,
                "ca-app-pub-3940256099942544~3347511713");

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


    }


    @Override
    protected void onPause() {
        super.onPause();
//       unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(DOC_LOADER_ID,null,this);
//        broadcastReceiver = new CardServiceBroadcastReceiver();
//        registerReceiver(broadcastReceiver,intentFilter);

    }

}
