package com.upwork.iurii.dms_uploader;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.upwork.iurii.dms_uploader.fragments.MainFragment;
import com.upwork.iurii.dms_uploader.fragments.QueueFragment;
import com.upwork.iurii.dms_uploader.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView navigationView;
    private Fragment lastFragment;
    private TextView navDeviceIDText;
    private Settings settings;

    private MainFragment fragment_main;
    private QueueFragment fragment_queue;
    private SettingsFragment fragment_settings;

    public TextView getNavDeviceIDText() {
        return navDeviceIDText;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = Settings.getInstance();

        fragment_main = new MainFragment();
        fragment_settings = new SettingsFragment();
        fragment_queue = new QueueFragment();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        fab.setVisibility(View.GONE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navDeviceIDText = (TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_device_id);
        navDeviceIDText.setText((String) settings.getPref(Settings.Pref.device_id));
    }

    @Override
    protected void onResume() {
        super.onResume();

        MyApplication.getInstance().setMainActivity(this);

        if (lastFragment == null) {
            navigationView.getMenu().performIdentifierAction(R.id.fragment_main, 0);
            navigationView.setCheckedItem(R.id.fragment_main);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        if (((String) settings.getPref(Settings.Pref.device_id)).isEmpty() || ((String) settings.getPref(Settings.Pref.doc_type)).isEmpty()) {
            id = R.id.fragment_settings;
            Toast.makeText(MainActivity.this, "Set parameters", Toast.LENGTH_LONG).show();
        }

        if (id == R.id.fragment_main) {
            lastFragment = fragment_main;
            fragmentTransaction.replace(R.id.container, fragment_main);
        } else if (id == R.id.fragment_queue) {
            lastFragment = fragment_queue;
            fragmentTransaction.replace(R.id.container, fragment_queue);
        } else if (id == R.id.fragment_settings) {
            lastFragment = fragment_settings;
            fragmentTransaction.replace(R.id.container, fragment_settings);
        }

        fragmentTransaction.commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onTestButtonClick(View view) {
        // TODO: 15.09.2016
    }

    public void openQueueFragment() {
        navigationView.getMenu().performIdentifierAction(R.id.fragment_queue, 0);
        navigationView.setCheckedItem(R.id.fragment_queue);
    }
}
