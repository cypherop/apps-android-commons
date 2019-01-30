package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.net.Uri;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.PlaceUtils;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.ContributionController.NEARBY_CAMERA_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.NEARBY_GALLERY_UPLOAD_REQUEST_CODE;
import static fr.free.nrw.commons.contributions.ContributionController.NEARBY_UPLOAD_IMAGE_LIMIT;
import static fr.free.nrw.commons.theme.NavigationBaseActivity.startActivityWithFlags;
import static fr.free.nrw.commons.wikidata.WikidataConstants.IS_DIRECT_UPLOAD;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ENTITY_ID_PREF;
import static fr.free.nrw.commons.wikidata.WikidataConstants.WIKIDATA_ITEM_LOCATION;

public class PlaceRenderer extends Renderer<Place> {

    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvDesc) TextView tvDesc;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.buttonLayout) LinearLayout buttonLayout;
    @BindView(R.id.cameraButton) LinearLayout cameraButton;

    @BindView(R.id.galleryButton) LinearLayout galleryButton;
    @BindView(R.id.directionsButton) LinearLayout directionsButton;
    @BindView(R.id.iconOverflow) LinearLayout iconOverflow;
    @BindView(R.id.cameraButtonText) TextView cameraButtonText;
    @BindView(R.id.galleryButtonText) TextView galleryButtonText;
    @BindView(R.id.bookmarkButton) LinearLayout bookmarkButton;
    @BindView(R.id.bookmarkButtonText) TextView bookmarkButtonText;
    @BindView(R.id.bookmarkButtonImage) ImageView bookmarkButtonImage;

    @BindView(R.id.directionsButtonText) TextView directionsButtonText;
    @BindView(R.id.iconOverflowText) TextView iconOverflowText;

    private View view;
    private static ArrayList<LinearLayout> openedItems;
    private Place place;

    private Fragment fragment;
    private ContributionController controller;
    private OnBookmarkClick onBookmarkClick;

    @Inject BookmarkLocationsDao bookmarkLocationDao;
    @Inject @Named("application_preferences") BasicKvStore applicationKvStore;
    @Inject @Named("defaultKvStore") BasicKvStore prefs;
    @Inject @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore;
    @Inject @Named("default_preferences") BasicKvStore defaultKvStore;

    public PlaceRenderer(){
        openedItems = new ArrayList<>();
    }

    public PlaceRenderer(
            Fragment fragment,
            ContributionController controller,
            OnBookmarkClick onBookmarkClick
    ) {
        this.fragment = fragment;
        this.controller = controller;
        openedItems = new ArrayList<>();
        this.onBookmarkClick = onBookmarkClick;
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        view = layoutInflater.inflate(R.layout.item_place, viewGroup, false);
        return view;
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
        closeLayout(buttonLayout);
    }

    @Override
    protected void hookListeners(View view) {
        if (onBookmarkClick != null) {
            final View.OnClickListener listener = view12 -> {
                Timber.d("Renderer clicked");
                TransitionManager.beginDelayedTransition(buttonLayout);

                if (buttonLayout.isShown()) {
                    closeLayout(buttonLayout);
                } else {
                    openLayout(buttonLayout);
                }

            };
            view.setOnClickListener(listener);

            view.requestFocus();
            view.setOnFocusChangeListener((view1, hasFocus) -> {
                if (!hasFocus && buttonLayout.isShown()) {
                    closeLayout(buttonLayout);
                } else if (hasFocus && !buttonLayout.isShown()) {
                    listener.onClick(view1);
                }
            });
        }else {
            final View.OnClickListener listener = view12 -> {
                Timber.d("Renderer clicked");
                TransitionManager.beginDelayedTransition(buttonLayout);

                if (buttonLayout.isShown()) {
                    closeLayout(buttonLayout);
                } else {
                    openLayout(buttonLayout);
                }
                ((NearbyMapFragment)((NearbyFragment)((NearbyListFragment)fragment).getParentFragment()).getChildFragmentManager().findFragmentByTag(NearbyMapFragment.class.getSimpleName())).setMapCenter(place);



            };
            view.setOnClickListener(listener);

            view.requestFocus();
            view.setOnFocusChangeListener((view1, hasFocus) -> {
                if (!hasFocus && buttonLayout.isShown()) {
                    closeLayout(buttonLayout);
                } else if (hasFocus && !buttonLayout.isShown()) {
                    listener.onClick(view1);
                }
            });
        }

        cameraButton.setOnClickListener(view2 -> {
            if (applicationKvStore.getBoolean("login_skipped", false)) {
                // prompt the user to login
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.login_alert_message)
                        .setPositiveButton(R.string.login, (dialog, which) -> {
                            startActivityWithFlags( getContext(), LoginActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            prefs.putBoolean("login_skipped", false);
                            fragment.getActivity().finish();
                        })
                        .show();
            } else {
                Timber.d("Camera button tapped. Image title: " + place.getName() + "Image desc: " + place.getLongDescription());
                storeSharedPrefs();
                controller.initiateCameraPick(fragment.getActivity(), NEARBY_CAMERA_UPLOAD_REQUEST_CODE);
            }
        });


        galleryButton.setOnClickListener(view3 -> {
            if (applicationKvStore.getBoolean("login_skipped", false)) {
                // prompt the user to login
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.login_alert_message)
                        .setPositiveButton(R.string.login, (dialog, which) -> {
                            startActivityWithFlags( getContext(), LoginActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            prefs.putBoolean("login_skipped", false);
                            fragment.getActivity().finish();
                        })
                        .show();
            }else {
                Timber.d("Gallery button tapped. Image title: " + place.getName() + "Image desc: " + place.getLongDescription());
                storeSharedPrefs();
                controller.initiateGalleryPick(fragment.getActivity(), NEARBY_UPLOAD_IMAGE_LIMIT, NEARBY_GALLERY_UPLOAD_REQUEST_CODE);
            }
        });

        bookmarkButton.setOnClickListener(view4 -> {
            if (applicationKvStore.getBoolean("login_skipped", false)) {
                // prompt the user to login
                new AlertDialog.Builder(getContext())
                        .setMessage(R.string.login_alert_message)
                        .setPositiveButton(R.string.login, (dialog, which) -> {
                            startActivityWithFlags( getContext(), LoginActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            prefs.putBoolean("login_skipped", false);
                            fragment.getActivity().finish();
                        })
                        .show();
            } else {
                boolean isBookmarked = bookmarkLocationDao.updateBookmarkLocation(place);
                int icon = isBookmarked ? R.drawable.ic_round_star_filled_24px : R.drawable.ic_round_star_border_24px;
                bookmarkButtonImage.setImageResource(icon);
                if (onBookmarkClick != null) {
                    onBookmarkClick.onClick();
                }
            }
        });
    }

    private void storeSharedPrefs() {
        Timber.d("Store place object %s", place.toString());
        directKvStore.putJson(PLACE_OBJECT, place);
    }

    private void closeLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.GONE);
    }

    private void openLayout(LinearLayout buttonLayout){
        buttonLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void render() {
        ApplicationlessInjection.getInstance(getContext().getApplicationContext())
                .getCommonsApplicationComponent().inject(this);
        place = getContent();
        tvName.setText(place.name);
        String descriptionText = place.getLongDescription();
        if (descriptionText.equals("?")) {
            descriptionText = getContext().getString(R.string.no_description_found);
            tvDesc.setVisibility(View.INVISIBLE);
        }
        tvDesc.setText(descriptionText);
        distance.setText(place.distance);
        icon.setImageResource(place.getLabel().getIcon());

        directionsButton.setOnClickListener(view -> {
            //Open map app at given position
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, place.location.getGmmIntentUri());
            if (mapIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                view.getContext().startActivity(mapIntent);
            }
        });

        iconOverflow.setVisibility(showMenu() ? View.VISIBLE : View.GONE);
        iconOverflow.setOnClickListener(v -> popupMenuListener());

        int icon;
        if (bookmarkLocationDao.findBookmarkLocation(place)) {
            icon = R.drawable.ic_round_star_filled_24px;
        } else {
            icon = R.drawable.ic_round_star_border_24px;
        }
        bookmarkButtonImage.setImageResource(icon);
    }

    private void popupMenuListener() {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), iconOverflow);
        popupMenu.inflate(R.menu.nearby_info_dialog_options);

        MenuItem commonsArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_commons_article);
        MenuItem wikiDataArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_wikidata_article);
        MenuItem wikipediaArticle = popupMenu.getMenu()
                .findItem(R.id.nearby_info_menu_wikipedia_article);

        commonsArticle.setEnabled(place.hasCommonsLink());
        wikiDataArticle.setEnabled(place.hasWikidataLink());
        wikipediaArticle.setEnabled(place.hasWikipediaLink());

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.nearby_info_menu_commons_article:
                    openWebView(place.siteLinks.getCommonsLink());
                    return true;
                case R.id.nearby_info_menu_wikidata_article:
                    openWebView(place.siteLinks.getWikidataLink());
                    return true;
                case R.id.nearby_info_menu_wikipedia_article:
                    openWebView(place.siteLinks.getWikipediaLink());
                    return true;
                default:
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void openWebView(Uri link) {
        Utils.handleWebUrl(getContext(), link);
    }

    private boolean showMenu() {
        return place.hasCommonsLink() || place.hasWikidataLink();
    }

    public interface OnBookmarkClick {
        void onClick();
    }

}
