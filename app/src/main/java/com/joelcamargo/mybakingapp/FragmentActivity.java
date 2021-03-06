package com.joelcamargo.mybakingapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.diegodobelo.expandingview.ExpandingItem;
import com.diegodobelo.expandingview.ExpandingList;
import com.google.gson.reflect.TypeToken;
import com.joelcamargo.mybakingapp.model.Ingredient;
import com.joelcamargo.mybakingapp.model.Recipe;

import java.lang.reflect.Type;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by joelcamargo on 12/19/17.
 */

@SuppressWarnings({"WeakerAccess", "DefaultFileTemplate"})
public class FragmentActivity extends AppCompatActivity
        implements RecipeInfoFragment.UpdateViewsInterface,
        StepsAdapter.StepRecyclerViewClickListener {

    // Recipe that will be instantiated once intent is received
    private static Recipe mReceivedRecipe;

    // finds all views needed for inflation and populating
    @BindView(R.id.fragmentToolbar)
    Toolbar mFragmentToolBar;
    @BindView(R.id.steps_recyclerView)
    RecyclerView mStepsRecyclerView;
    @BindView(R.id.ingredientsRecyclerView)
    RecyclerView mIngredientsRecyclerView;
    @Nullable
    @BindView(R.id.empty_view_step_info)
    TextView mEmptyViewStepInfo;
    @BindView(R.id.heart_imageView)
    ImageView mHeartImageView;

    // this imageView doesn't work with butterknife since its a part of 3rd party expandingView
    private ImageView mListArrow;

    // * * THIS IS DIEGOS EXPANDING STUFF
    ExpandingList mExpandingList;
    ExpandingItem mItem;
    // * * * *

    StepsAdapter mStepsAdapter;
    LinearLayoutManager mLinearLayoutManager;
    RecipeInfoFragment mRecipeFragment;
    StepInfoFragment mStepInfoFragment;

    RotateAnimation mAnim;
    RotateAnimation mAnim2;
    private long mFavoriteRecipeId = 0;
    private Toast mToast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_activity);

        ButterKnife.bind(this);
        setSupportActionBar(mFragmentToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // gets shared prefs and sets most recent favorite recipe id
        SharedPreferences settings = getSharedPreferences("prefs", 0);
        mFavoriteRecipeId = settings.getLong("faveRecipeId", 0);

        // Gets the bundle containing our Recipe object
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            mReceivedRecipe = bundle.getParcelable("recipe");
        } else {
            Log.e("ERROR!!", " NO RECIPE INFO RECEIVED");
        }

        // Creates and applies the RecipeInfoFragment
        mRecipeFragment = new RecipeInfoFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction()
                .add(mRecipeFragment, "currentRecipe");
        ft.commit();


        // applies the settings for our layoutManager and sets it on the recyclerView
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mStepsRecyclerView.setLayoutManager(mLinearLayoutManager);

        // instantiates the steps adapter and applies to steps recyclerview
        mStepsAdapter = new StepsAdapter(getApplicationContext(), mReceivedRecipe, this);
        mStepsRecyclerView.setAdapter(mStepsAdapter);
        updateInfoViews(mReceivedRecipe);
        mStepsAdapter.notifyDataSetChanged();

        // instantiates animation for imageView
        makeAnims();
    }

    // interface implemented from RecipeInfoFragment to update views in that fragment
    private void updateInfoViews(Recipe recipe) {
        // Sets the recipe name in the toolbar
        String recipeName = mReceivedRecipe.getName();
        mFragmentToolBar.setTitle(recipeName);
        mFragmentToolBar.setTitleTextAppearance(getApplicationContext(), R.style.ToolBar_Text_Font);

        // sets heart imageView according to value saved in shared prefs
        if (mReceivedRecipe.getId() == mFavoriteRecipeId) {
            mHeartImageView.setImageResource(R.drawable.ic_favorite__heart_filled_24dp);
            mHeartImageView.setColorFilter(getResources().getColor(R.color.red));
        }


        mHeartImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mToast != null) {
                    mToast.cancel();
                }

                if (mFavoriteRecipeId != mReceivedRecipe.getId()) {
                    mHeartImageView.setImageResource(R.drawable.ic_favorite__heart_filled_24dp);
                    mHeartImageView.setColorFilter(getResources().getColor(R.color.red));
                    mFavoriteRecipeId = mReceivedRecipe.getId();

                    mToast = Toast.makeText(getApplicationContext(),
                            "Added to widget",
                            Toast.LENGTH_SHORT);
                    mToast.show();

                    // updates recipe on widget
                    WidgetUpdatingService.startActionUpdateWidget(getApplicationContext(), mReceivedRecipe);

                } else {

                    if (mToast != null) {
                        mToast.cancel();
                    }

                    mHeartImageView.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    mHeartImageView.setColorFilter(getResources().getColor(R.color.white));
                    mFavoriteRecipeId = 0;

                    mToast = Toast.makeText(getApplicationContext(),
                            "Will stay on widget until new favorite is selected",
                            Toast.LENGTH_LONG);

                    mToast.show();
                }

            }
        });

        // Gets the ingredient array to create the sub-items below
        ArrayList<Ingredient> ingredientArrayList = (ArrayList<Ingredient>) recipe.getIngredients();
        int ingredientArraySize = ingredientArrayList.size();
        Log.d("ARRAYSIZE", " " + ingredientArraySize);

        // Creates expanding list
        mExpandingList = findViewById(R.id.expanding_list_diego);

        // Creates the items and applies the data into our expandingLists views
        mItem = mExpandingList.createNewItem(R.layout.expanding_layout);
        TextView expandable_title = mItem.findViewById(R.id.title_ingredients_list);
        expandable_title.setText(R.string.ingredient_list_header);
        mItem.createSubItems(ingredientArraySize);
        mListArrow = findViewById(R.id.listArrow);
        mListArrow.setBackgroundResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
        // sets initial anim
        mListArrow.setAnimation(mAnim);
        mItem.setStateChangedListener(new ExpandingItem.OnItemStateChanged() {
            @Override
            public void itemCollapseStateChanged(boolean expanded) {
                if (expanded){
                    mListArrow.startAnimation(mAnim);
                    mListArrow.setAnimation(mAnim2);
                } else {
                    mListArrow.startAnimation(mAnim2);
                    mListArrow.setAnimation(mAnim);
                }
            }
        });

        // for loop to iterate through all ingredients and populate sub item textViews
        for (int position = 0; position < ingredientArraySize; position++) {
            // Ingredient being used for current sub item position
            Ingredient currentIngredient = ingredientArrayList.get(position);
            String ingredientName = currentIngredient.getIngredient();
            String ingredientMeasure = currentIngredient.getMeasure();
            String ingredientQuantity = String.valueOf(currentIngredient.getQuantity());
            String fullMeasureString = ingredientQuantity + " " + ingredientMeasure + " " + ingredientName;
            Log.d("current ingredient is: ", fullMeasureString);

            View currentSubItem = mItem.getSubItemView(position);

            ((TextView) currentSubItem.findViewById(R.id.measure_textView))
                    .setText(getString(R.string.measure_string_format, ingredientQuantity, ingredientMeasure));

            ((TextView) currentSubItem.findViewById(R.id.ingredientNameTextView))
                    .setText(ingredientName);

            if (ingredientName.length() > 45) {
                ((TextView) currentSubItem.findViewById(R.id.ingredientNameTextView)).setTextSize(14);
                ((TextView) currentSubItem.findViewById(R.id.ingredientNameTextView)).setMinLines(2);
            }
        }
    }

    // helper method that opens the step fragment for clicked step item
    private void displayStepInfoFragment(int clickedPosition) {

        if (!MainActivity.mTwoPane) {
            mStepInfoFragment = new StepInfoFragment();
            Bundle args = new Bundle();
            args.putParcelable("recipe", mReceivedRecipe);
            args.putInt("position", clickedPosition);
            mStepInfoFragment.setArguments(args);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction()
                    .add(R.id.mainFragmentContainer, mStepInfoFragment, "step_tag")
                    .addToBackStack("step_backStack")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
        } else {

            if (mEmptyViewStepInfo != null) {
                mEmptyViewStepInfo.setVisibility(View.INVISIBLE);
            }

            FragmentManager fm = getSupportFragmentManager();

            StepInfoFragment oldFragToRemove =
                    (StepInfoFragment) fm.findFragmentById(R.id.step_info_fragment_container);

            mStepInfoFragment = new StepInfoFragment();
            Bundle args = new Bundle();
            args.putParcelable("recipe", mReceivedRecipe);
            args.putInt("position", clickedPosition);
            mStepInfoFragment.setArguments(args);

            if (oldFragToRemove == null) {
                FragmentTransaction ft = fm.beginTransaction()
                        .add(R.id.step_info_fragment_container, mStepInfoFragment, "step_tag")
                        .addToBackStack("step_backStack")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.commit();
            } else {
                FragmentTransaction ft = fm.beginTransaction()
                        .add(R.id.step_info_fragment_container, mStepInfoFragment, "step_tag")
                        .show(mStepInfoFragment)
                        .hide(oldFragToRemove)
                        .remove(oldFragToRemove)
                        .addToBackStack("step_backStack")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.commit();
            }

        }

    }

    @Override
    public void stepRecyclerViewListItemCLick(View v, int position) {
        displayStepInfoFragment(position);
    }

    // updates anim to updated drawable for the List Arrow imageView
    private void makeAnims() {
        mAnim = new RotateAnimation(180, 0, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mAnim.setDuration(300);
        mAnim.setInterpolator(new LinearInterpolator());
        mAnim.setFillAfter(true);


        mAnim2 = new RotateAnimation(0, 180, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mAnim2.setDuration(300);
        mAnim2.setInterpolator(new LinearInterpolator());
        mAnim2.setFillAfter(true);
    }

    // closes the currently opened fragment and pops the backstack, if any
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!MainActivity.mTwoPane) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentById(R.id.mainFragmentContainer) != null) {
                fm.beginTransaction().remove(fm.findFragmentById(R.id.mainFragmentContainer)).commit();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // creates Editor object to make preference changes.
        SharedPreferences settings = getSharedPreferences("prefs", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("faveRecipeId", mFavoriteRecipeId);
        Type type = new TypeToken<Recipe>() {
        }.getType();
        editor.putString("recipe", ConverterHelper.convertToJsonString(mReceivedRecipe, type));

        // Commit the edits!
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
