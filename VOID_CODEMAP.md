# Void feature integration map
## Drawer and gestures
src/com/android/launcher3/touch/AllAppsSwipeController.java:25:import static com.android.launcher3.LauncherState.ALL_APPS;
src/com/android/launcher3/touch/AllAppsSwipeController.java:27:import static com.android.launcher3.states.StateAnimationConfig.ANIM_ALL_APPS_FADE;
src/com/android/launcher3/touch/AllAppsSwipeController.java:48: * TouchController to switch between NORMAL and ALL_APPS state.
src/com/android/launcher3/touch/AllAppsSwipeController.java:52:    private static final float ALL_APPS_CONTENT_FADE_MAX_CLAMPING_THRESHOLD = 0.8f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:53:    private static final float ALL_APPS_CONTENT_FADE_MIN_CLAMPING_THRESHOLD = 0.5f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:54:    private static final float ALL_APPS_SCRIM_VISIBLE_THRESHOLD = 0.1f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:55:    private static final float ALL_APPS_STAGGERED_FADE_THRESHOLD = 0.5f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:57:    private static final Interpolator ALL_APPS_SCRIM_RESPONDER =
src/com/android/launcher3/touch/AllAppsSwipeController.java:59:                    LINEAR, ALL_APPS_SCRIM_VISIBLE_THRESHOLD, ALL_APPS_STAGGERED_FADE_THRESHOLD);
src/com/android/launcher3/touch/AllAppsSwipeController.java:60:    private static final Interpolator ALL_APPS_CLAMPING_RESPONDER =
src/com/android/launcher3/touch/AllAppsSwipeController.java:63:                    1 - ALL_APPS_CONTENT_FADE_MAX_CLAMPING_THRESHOLD,
src/com/android/launcher3/touch/AllAppsSwipeController.java:64:                    1 - ALL_APPS_CONTENT_FADE_MIN_CLAMPING_THRESHOLD);
src/com/android/launcher3/touch/AllAppsSwipeController.java:67:    private static final Interpolator ALL_APPS_SHEET_DEPTH = DECELERATED_EASE;
src/com/android/launcher3/touch/AllAppsSwipeController.java:69:    // ---- Custom interpolators for NORMAL -> ALL_APPS on phones only. ----
src/com/android/launcher3/touch/AllAppsSwipeController.java:71:    public static final float ALL_APPS_STATE_TRANSITION_ATOMIC = 0.3333f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:72:    public static final float ALL_APPS_STATE_TRANSITION_MANUAL = 0.4f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:73:    private static final float ALL_APPS_FADE_END_ATOMIC = 0.8333f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:74:    private static final float ALL_APPS_FADE_END_MANUAL = 0.8f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:75:    private static final float ALL_APPS_FULL_DEPTH_PROGRESS = 0.5f;
src/com/android/launcher3/touch/AllAppsSwipeController.java:81:            Interpolators.clampToProgress(LINEAR, 0f, ALL_APPS_STATE_TRANSITION_MANUAL);
src/com/android/launcher3/touch/AllAppsSwipeController.java:83:            Interpolators.clampToProgress(FINAL_FRAME, 0f, ALL_APPS_STATE_TRANSITION_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:85:            Interpolators.clampToProgress(FINAL_FRAME, 0f, ALL_APPS_STATE_TRANSITION_MANUAL);
src/com/android/launcher3/touch/AllAppsSwipeController.java:89:            Interpolators.mapToProgress(LINEAR, 0f, ALL_APPS_FULL_DEPTH_PROGRESS);
src/com/android/launcher3/touch/AllAppsSwipeController.java:92:                    BLUR_ADJUSTED, WORKSPACE_MOTION_START_ATOMIC, ALL_APPS_STATE_TRANSITION_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:94:            Interpolators.clampToProgress(BLUR_ADJUSTED, 0f, ALL_APPS_STATE_TRANSITION_MANUAL);
src/com/android/launcher3/touch/AllAppsSwipeController.java:102:                    ALL_APPS_STATE_TRANSITION_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:111:                    ALL_APPS_STATE_TRANSITION_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:120:                    SCRIM_FADE_START_ATOMIC, ALL_APPS_STATE_TRANSITION_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:123:                    LINEAR, SCRIM_FADE_START_MANUAL, ALL_APPS_STATE_TRANSITION_MANUAL);
src/com/android/launcher3/touch/AllAppsSwipeController.java:125:    public static final Interpolator ALL_APPS_FADE_ATOMIC =
src/com/android/launcher3/touch/AllAppsSwipeController.java:128:                    ALL_APPS_STATE_TRANSITION_ATOMIC, ALL_APPS_FADE_END_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:129:    public static final Interpolator ALL_APPS_FADE_MANUAL =
src/com/android/launcher3/touch/AllAppsSwipeController.java:131:                    LINEAR, ALL_APPS_STATE_TRANSITION_MANUAL, ALL_APPS_FADE_END_MANUAL);
src/com/android/launcher3/touch/AllAppsSwipeController.java:133:    public static final Interpolator ALL_APPS_VERTICAL_PROGRESS_ATOMIC =
src/com/android/launcher3/touch/AllAppsSwipeController.java:136:                    ALL_APPS_STATE_TRANSITION_ATOMIC, 1f);
src/com/android/launcher3/touch/AllAppsSwipeController.java:137:    public static final Interpolator ALL_APPS_VERTICAL_PROGRESS_MANUAL = LINEAR;
src/com/android/launcher3/touch/AllAppsSwipeController.java:142:        super(l, SingleAxisSwipeDetector.VERTICAL);
src/com/android/launcher3/touch/AllAppsSwipeController.java:154:        if (!mLauncher.isInState(NORMAL) && !mLauncher.isInState(ALL_APPS)) {
src/com/android/launcher3/touch/AllAppsSwipeController.java:158:        if (mLauncher.isInState(ALL_APPS) && !mLauncher.getAppsView().shouldContainerScroll(ev)) {
src/com/android/launcher3/touch/AllAppsSwipeController.java:167:            return ALL_APPS;
src/com/android/launcher3/touch/AllAppsSwipeController.java:168:        } else if (fromState == ALL_APPS && !isDragTowardPositive) {
src/com/android/launcher3/touch/AllAppsSwipeController.java:193:        if (fromState == NORMAL && toState == ALL_APPS) {
src/com/android/launcher3/touch/AllAppsSwipeController.java:195:        } else if (fromState == ALL_APPS && toState == NORMAL) {
src/com/android/launcher3/touch/AllAppsSwipeController.java:207:                    Interpolators.reverse(ALL_APPS_SCRIM_RESPONDER));
src/com/android/launcher3/touch/AllAppsSwipeController.java:208:            config.setInterpolator(ANIM_ALL_APPS_FADE, FINAL_FRAME);
src/com/android/launcher3/touch/AllAppsSwipeController.java:213:                Interpolators.reverse(ALL_APPS_SHEET_DEPTH));
src/com/android/launcher3/touch/AllAppsSwipeController.java:214:            config.setInterpolator(ANIM_HOTSEAT_SCALE, Interpolators.reverse(ALL_APPS_SHEET_DEPTH));
src/com/android/launcher3/touch/AllAppsSwipeController.java:215:            config.setInterpolator(ANIM_DEPTH, Interpolators.reverse(ALL_APPS_SHEET_DEPTH));
src/com/android/launcher3/touch/AllAppsSwipeController.java:236:                config.setInterpolator(ANIM_ALL_APPS_FADE,
src/com/android/launcher3/touch/AllAppsSwipeController.java:237:                        Interpolators.reverse(ALL_APPS_FADE_MANUAL));
src/com/android/launcher3/touch/AllAppsSwipeController.java:239:                        Interpolators.reverse(ALL_APPS_VERTICAL_PROGRESS_MANUAL));
src/com/android/launcher3/touch/AllAppsSwipeController.java:242:                        Interpolators.reverse(ALL_APPS_SCRIM_RESPONDER));
src/com/android/launcher3/touch/AllAppsSwipeController.java:243:                config.setInterpolator(ANIM_ALL_APPS_FADE, ALL_APPS_CLAMPING_RESPONDER);
src/com/android/launcher3/touch/AllAppsSwipeController.java:256:            config.setInterpolator(ANIM_ALL_APPS_FADE, INSTANT);
src/com/android/launcher3/touch/AllAppsSwipeController.java:257:            config.setInterpolator(ANIM_SCRIM_FADE, ALL_APPS_SCRIM_RESPONDER);
src/com/android/launcher3/touch/AllAppsSwipeController.java:261:            config.setInterpolator(ANIM_WORKSPACE_SCALE, ALL_APPS_SHEET_DEPTH);
src/com/android/launcher3/touch/AllAppsSwipeController.java:262:            config.setInterpolator(ANIM_HOTSEAT_SCALE, ALL_APPS_SHEET_DEPTH);
src/com/android/launcher3/touch/AllAppsSwipeController.java:263:            config.setInterpolator(ANIM_DEPTH, ALL_APPS_SHEET_DEPTH);
src/com/android/launcher3/touch/AllAppsSwipeController.java:287:            config.setInterpolator(ANIM_ALL_APPS_FADE,
src/com/android/launcher3/touch/AllAppsSwipeController.java:288:                    config.isUserControlled() ? ALL_APPS_FADE_MANUAL : ALL_APPS_FADE_ATOMIC);
src/com/android/launcher3/touch/AllAppsSwipeController.java:291:                            ? ALL_APPS_VERTICAL_PROGRESS_MANUAL
src/com/android/launcher3/touch/AllAppsSwipeController.java:292:                            : ALL_APPS_VERTICAL_PROGRESS_ATOMIC);
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:22:import static com.android.launcher3.LauncherState.ALL_APPS;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:55:        implements TouchController, SingleAxisSwipeDetector.Listener {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:58:    protected final SingleAxisSwipeDetector mDetector;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:59:    protected final SingleAxisSwipeDetector.Direction mSwipeDirection;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:83:    public AbstractStateChangeTouchController(Launcher l, SingleAxisSwipeDetector.Direction dir) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:85:        mDetector = new SingleAxisSwipeDetector(l, this, dir);
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:108:                directionsToDetectScroll = SingleAxisSwipeDetector.DIRECTION_BOTH;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:139:            swipeDirection |= SingleAxisSwipeDetector.DIRECTION_POSITIVE;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:142:            swipeDirection |= SingleAxisSwipeDetector.DIRECTION_NEGATIVE;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:227:            if (mFromState == LauncherState.ALL_APPS) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:238:            if (mToState == LauncherState.ALL_APPS) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:255:            if (mStartState == ALL_APPS) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:319:                    && (mToState == ALL_APPS || mFromState == ALL_APPS)) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:322:                    && mToState == ALL_APPS && mFromState == NORMAL) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:323:                successTransitionProgress = AllAppsSwipeController.ALL_APPS_STATE_TRANSITION_MANUAL;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:325:                    && mToState == NORMAL && mFromState == ALL_APPS) {
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:327:                        1 - AllAppsSwipeController.ALL_APPS_STATE_TRANSITION_MANUAL;
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:348:                duration = BaseSwipeDetector.calculateDuration(velocity,
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:364:                duration = BaseSwipeDetector.calculateDuration(velocity,
src/com/android/launcher3/touch/AbstractStateChangeTouchController.java:373:        if (targetState == LauncherState.ALL_APPS) {
src/com/android/launcher3/touch/ItemLongClickListener.java:21:import static com.android.launcher3.LauncherState.ALL_APPS;
src/com/android/launcher3/touch/ItemLongClickListener.java:60:    public static final OnLongClickListener INSTANCE_ALL_APPS =
src/com/android/launcher3/touch/ItemLongClickListener.java:150:        if (!launcher.isInState(ALL_APPS) && !launcher.isInState(OVERVIEW)) return false;
src/com/android/launcher3/touch/SingleAxisSwipeDetector.java:31:public class SingleAxisSwipeDetector extends BaseSwipeDetector {
src/com/android/launcher3/touch/SingleAxisSwipeDetector.java:107:    public SingleAxisSwipeDetector(@NonNull Context context, @NonNull Listener l,
src/com/android/launcher3/touch/SingleAxisSwipeDetector.java:115:    protected SingleAxisSwipeDetector(@NonNull Context context, @NonNull ViewConfiguration config,
src/com/android/launcher3/touch/BothAxesSwipeDetector.java:30:public class BothAxesSwipeDetector extends BaseSwipeDetector {
src/com/android/launcher3/touch/BothAxesSwipeDetector.java:44:    public BothAxesSwipeDetector(@NonNull Context context, @NonNull Listener l) {
src/com/android/launcher3/touch/WorkspaceTouchListener.java:26:import static com.android.launcher3.LauncherState.ALL_APPS;
src/com/android/launcher3/touch/WorkspaceTouchListener.java:164:        boolean isInAllAppsBottomSheet = mLauncher.isInState(ALL_APPS)
src/com/android/launcher3/touch/WorkspaceTouchListener.java:207:                    .withSrcState(ALL_APPS.statsLogOrdinal)
src/com/android/launcher3/touch/BaseSwipeDetector.java:44: * @see SingleAxisSwipeDetector
src/com/android/launcher3/touch/BaseSwipeDetector.java:45: * @see BothAxesSwipeDetector
src/com/android/launcher3/touch/BaseSwipeDetector.java:47:public abstract class BaseSwipeDetector {
src/com/android/launcher3/touch/BaseSwipeDetector.java:50:    private static final String TAG = "BaseSwipeDetector";
src/com/android/launcher3/touch/BaseSwipeDetector.java:80:    protected BaseSwipeDetector(@NonNull Context context, @NonNull ViewConfiguration config,
src/com/android/launcher3/recyclerview/AllAppsRecyclerViewPool.kt:178:     * Note that if [FeatureFlags.ALL_APPS_GONE_VISIBILITY] is enabled, we need to preinfate extra
src/com/android/launcher3/SecondaryDropTarget.java:168:                    || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false);
src/com/android/launcher3/Launcher.java:53:import static com.android.launcher3.LauncherConstants.TraceEvents.DISPLAY_ALL_APPS_TRACE_METHOD_NAME;
src/com/android/launcher3/Launcher.java:64:import static com.android.launcher3.LauncherState.ALL_APPS;
src/com/android/launcher3/Launcher.java:174:import com.android.launcher3.allapps.AllAppsTransitionController;
src/com/android/launcher3/Launcher.java:317:    public static final String INTENT_ACTION_ALL_APPS_TOGGLE =
src/com/android/launcher3/Launcher.java:361:    AllAppsTransitionController mAllAppsController;
src/com/android/launcher3/Launcher.java:445://        Trace.beginAsyncSection(DISPLAY_ALL_APPS_TRACE_METHOD_NAME, SINGLE_TRACE_COOKIE);
src/com/android/launcher3/Launcher.java:463:        mAllAppsController = new AllAppsTransitionController(this);
src/com/android/launcher3/Launcher.java:1159:        if (mPrevLauncherState != state && ALL_APPS.equals(state)
src/com/android/launcher3/Launcher.java:1210:        if (ALL_APPS.equals(mPrevLauncherState) && !ALL_APPS.equals(state)
src/com/android/launcher3/Launcher.java:1358:        mAppsView.setAllAppsTransitionController(mAllAppsController);
src/com/android/launcher3/Launcher.java:1657:        } else if (Intent.ACTION_ALL_APPS.equals(intent.getAction())) {
src/com/android/launcher3/Launcher.java:1659:        } else if (INTENT_ACTION_ALL_APPS_TOGGLE.equals(intent.getAction())) {
src/com/android/launcher3/Launcher.java:1686:        if (getStateManager().isInStableState(ALL_APPS)) {
src/com/android/launcher3/Launcher.java:1693:            getStateManager().goToState(ALL_APPS, true /* animated */,
src/com/android/launcher3/Launcher.java:1716:        getStateManager().goToState(ALL_APPS, alreadyOnHome);
src/com/android/launcher3/Launcher.java:2448:            Trace.endAsyncSection(DISPLAY_ALL_APPS_TRACE_METHOD_NAME, SINGLE_TRACE_COOKIE);
src/com/android/launcher3/Launcher.java:2648:                && getStateManager().getState() != LauncherState.ALL_APPS;
src/com/android/launcher3/Launcher.java:2894:    public AllAppsTransitionController getAllAppsController() {
src/com/android/launcher3/Launcher.java:3064:        return ItemLongClickListener.INSTANCE_ALL_APPS;
src/com/android/launcher3/model/ModelLauncherCallbacks.kt:129:        if (FeatureFlags.PROMISE_APPS_IN_ALL_APPS.get()) {
src/com/android/launcher3/model/data/AppInfo.java:19:import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS;
src/com/android/launcher3/model/data/AppInfo.java:101:        this.container = CONTAINER_ALL_APPS;
src/com/android/launcher3/model/data/ItemInfo.java:19:import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS;
src/com/android/launcher3/model/data/ItemInfo.java:23:import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION;
src/com/android/launcher3/model/data/ItemInfo.java:337:                || container == CONTAINER_ALL_APPS_PREDICTION;
src/com/android/launcher3/model/data/ItemInfo.java:474:            case CONTAINER_ALL_APPS:
src/com/android/launcher3/model/data/ItemInfo.java:484:            case CONTAINER_ALL_APPS_PREDICTION:
src/com/android/launcher3/model/data/LauncherAppWidgetInfo.java:19:import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS;
src/com/android/launcher3/model/data/LauncherAppWidgetInfo.java:266:            case CONTAINER_ALL_APPS:
src/com/android/launcher3/model/data/LauncherAppWidgetInfo.java:267:                return LauncherAtom.Attribute.ALL_APPS_SEARCH_RESULT_WIDGETS;
src/com/android/launcher3/model/PackageUpdatedTask.java:138:                    if (FeatureFlags.PROMISE_APPS_IN_ALL_APPS.get()) {
src/com/android/launcher3/model/PackageUpdatedTask.java:140:                            Log.d(TAG, "OP_ADD: PROMISE_APPS_IN_ALL_APPS enabled:"
src/com/android/launcher3/model/LoaderTask.java:682:        if (FeatureFlags.PROMISE_APPS_IN_ALL_APPS.get()) {
src/com/android/launcher3/model/StringCache.java:82:    private static final String ALL_APPS_WORK_TAB = PREFIX + "ALL_APPS_WORK_TAB";
src/com/android/launcher3/model/StringCache.java:87:    private static final String ALL_APPS_PERSONAL_TAB = PREFIX + "ALL_APPS_PERSONAL_TAB";
src/com/android/launcher3/model/StringCache.java:92:    private static final String ALL_APPS_WORK_TAB_ACCESSIBILITY =
src/com/android/launcher3/model/StringCache.java:93:            PREFIX + "ALL_APPS_WORK_TAB_ACCESSIBILITY";
src/com/android/launcher3/model/StringCache.java:98:    private static final String ALL_APPS_PERSONAL_TAB_ACCESSIBILITY =
src/com/android/launcher3/model/StringCache.java:99:            PREFIX + "ALL_APPS_PERSONAL_TAB_ACCESSIBILITY";
src/com/android/launcher3/model/StringCache.java:206:                context, ALL_APPS_WORK_TAB, R.string.all_apps_work_tab);
src/com/android/launcher3/model/StringCache.java:208:                context, ALL_APPS_PERSONAL_TAB, R.string.all_apps_personal_tab);
src/com/android/launcher3/model/StringCache.java:210:                context, ALL_APPS_WORK_TAB_ACCESSIBILITY, R.string.all_apps_button_work_label);
src/com/android/launcher3/model/StringCache.java:212:                context, ALL_APPS_PERSONAL_TAB_ACCESSIBILITY,
src/com/android/launcher3/BubbleTextView.java:132:    public static final int DISPLAY_ALL_APPS = 1;
src/com/android/launcher3/BubbleTextView.java:290:        } else if (mDisplay == DISPLAY_ALL_APPS || mDisplay == DISPLAY_PREDICTION_ROW
src/com/android/launcher3/BubbleTextView.java:580:        if (mDisplay == DISPLAY_ALL_APPS || mDisplay == DISPLAY_DRAWER_FOLDER
src/com/android/launcher3/BubbleTextView.java:596:        if (mDisplay == DISPLAY_ALL_APPS || mDisplay == DISPLAY_PREDICTION_ROW) {
src/com/android/launcher3/BubbleTextView.java:614:            case DISPLAY_ALL_APPS, DISPLAY_PREDICTION_ROW -> {
src/com/android/launcher3/BubbleTextView.java:1382:            if (mDisplay == DISPLAY_ALL_APPS || mDisplay == DISPLAY_PREDICTION_ROW) {
src/com/android/launcher3/allapps/AllAppsRecyclerView.java:119:        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_ALL_APPS_DIVIDER, 1);
src/com/android/launcher3/allapps/BaseAllAppsAdapter.java:66:    public static final int VIEW_TYPE_ALL_APPS_DIVIDER = 1 << 3;
src/com/android/launcher3/allapps/BaseAllAppsAdapter.java:79:    public static final int VIEW_TYPE_MASK_DIVIDER = VIEW_TYPE_ALL_APPS_DIVIDER;
src/com/android/launcher3/allapps/BaseAllAppsAdapter.java:250:            case VIEW_TYPE_ALL_APPS_DIVIDER, VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER:
src/com/android/launcher3/allapps/BaseAllAppsAdapter.java:355:            case VIEW_TYPE_ALL_APPS_DIVIDER:
src/com/android/launcher3/allapps/AllAppsTransitionController.java:22:import static com.android.launcher3.LauncherState.ALL_APPS;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:23:import static com.android.launcher3.LauncherState.ALL_APPS_CONTENT;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:30:import static com.android.launcher3.states.StateAnimationConfig.ANIM_ALL_APPS_FADE;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:34:import static com.android.launcher3.util.SystemUiController.UI_STATE_ALL_APPS;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:82:public class AllAppsTransitionController
src/com/android/launcher3/allapps/AllAppsTransitionController.java:86:    public static final int REVERT_SWIPE_ALL_APPS_TO_HOME_ANIMATION_DURATION_MS = 200;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:90:            1 - AllAppsSwipeController.ALL_APPS_STATE_TRANSITION_MANUAL;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:92:    public static final FloatProperty<AllAppsTransitionController> ALL_APPS_PROGRESS =
src/com/android/launcher3/allapps/AllAppsTransitionController.java:93:            new FloatProperty<AllAppsTransitionController>("allAppsProgress") {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:96:                public Float get(AllAppsTransitionController controller) {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:101:                public void setValue(AllAppsTransitionController controller, float progress) {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:106:    private static final float ALL_APPS_PULL_BACK_TRANSLATION_DEFAULT = 0f;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:108:    public static final FloatProperty<AllAppsTransitionController> ALL_APPS_PULL_BACK_TRANSLATION =
src/com/android/launcher3/allapps/AllAppsTransitionController.java:109:            new FloatProperty<AllAppsTransitionController>("allAppsPullBackTranslation") {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:112:                public Float get(AllAppsTransitionController controller) {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:121:                public void setValue(AllAppsTransitionController controller, float translation) {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:125:                                ALL_APPS_PULL_BACK_TRANSLATION_DEFAULT);
src/com/android/launcher3/allapps/AllAppsTransitionController.java:129:                                ALL_APPS_PULL_BACK_TRANSLATION_DEFAULT);
src/com/android/launcher3/allapps/AllAppsTransitionController.java:134:    private static final float ALL_APPS_PULL_BACK_ALPHA_DEFAULT = 1f;
src/com/android/launcher3/allapps/AllAppsTransitionController.java:136:    public static final FloatProperty<AllAppsTransitionController> ALL_APPS_PULL_BACK_ALPHA =
src/com/android/launcher3/allapps/AllAppsTransitionController.java:137:            new FloatProperty<AllAppsTransitionController>("allAppsPullBackAlpha") {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:140:                public Float get(AllAppsTransitionController controller) {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:149:                public void setValue(AllAppsTransitionController controller, float alpha) {
src/com/android/launcher3/allapps/AllAppsTransitionController.java:153:                                ALL_APPS_PULL_BACK_ALPHA_DEFAULT);
src/com/android/launcher3/allapps/AllAppsTransitionController.java:157:                                ALL_APPS_PULL_BACK_ALPHA_DEFAULT);
src/com/android/launcher3/allapps/AllAppsTransitionController.java:196:    public AllAppsTransitionController(Launcher l) {
## Lifecycle and touch
src/com/android/launcher3/Launcher.java:1011:    protected void onStop() {
src/com/android/launcher3/Launcher.java:1012:        super.onStop();
src/com/android/launcher3/Launcher.java:1233:    protected void onResume() {
src/com/android/launcher3/Launcher.java:1235:        super.onResume();
src/com/android/launcher3/Launcher.java:1248:    protected void onPause() {
src/com/android/launcher3/Launcher.java:1252:        super.onPause();
src/com/android/launcher3/Launcher.java:2090:    public boolean dispatchTouchEvent(MotionEvent ev) {
src/com/android/launcher3/Launcher.java:2103:        return super.dispatchTouchEvent(ev);
src/com/android/launcher3/widget/LauncherAppWidgetHostView.java:258:        mLongPressHelper.onTouchEvent(ev);
src/com/android/launcher3/widget/LauncherAppWidgetHostView.java:263:    public boolean onTouchEvent(MotionEvent ev) {
src/com/android/launcher3/widget/LauncherAppWidgetHostView.java:264:        mLongPressHelper.onTouchEvent(ev);
src/com/android/launcher3/LauncherConstants.java:31:        public static final String ON_RESUME_EVT = "Launcher.onResume";
src/com/android/launcher3/secondarydisplay/SecondaryDisplayLauncher.java:123:    protected void onPause() {
src/com/android/launcher3/secondarydisplay/SecondaryDisplayLauncher.java:124:        super.onPause();
quickstep/src/com/android/quickstep/LauncherBackAnimationController.java:543:        // Launcher#onResumed, but in the predictive back flow launcher is not resumed until
quickstep/src/com/android/quickstep/util/LauncherUnfoldAnimationController.java:115:    public void onResume() {
quickstep/src/com/android/quickstep/util/LauncherUnfoldAnimationController.java:142:    public void onPause() {
quickstep/src/com/android/quickstep/views/LauncherRecentsView.java:221:    public boolean onTouchEvent(MotionEvent ev) {
quickstep/src/com/android/quickstep/views/LauncherRecentsView.java:222:        boolean result = super.onTouchEvent(ev);
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:916:    protected void onResume() {
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:917:        super.onResume();
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:922:            mLauncherUnfoldAnimationController.onResume();
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:931:    protected void onPause() {
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:933:            mLauncherUnfoldAnimationController.onPause();
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:936:        super.onPause();
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:953:    protected void onStop() {
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:954:        super.onStop();
quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java:1420:        // When changing screens, force moving to rest state similar to StatefulActivity.onStop, as
quickstep/src/com/android/launcher3/taskbar/LauncherTaskbarUIController.java:219:     * Should be called from onResume() and onPause(), and animates the Taskbar accordingly.
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:169:    public final void onResume() {
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:175:                        mOverlay.onResume();
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:185:    public final void onPause() {
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:191:                        mOverlay.onPause();
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:217:    public final void onStop() {
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:306:                    mOverlay.onResume();
lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java:308:                    mOverlay.onPause();
lawnchair/src/app/lawnchair/LawnchairLauncher.kt:470:    override fun onResume() {
lawnchair/src/app/lawnchair/LawnchairLauncher.kt:471:        super.onResume()
## Settings
lawnchair/src/app/lawnchair/predictions/LawnchairPredictionManager.kt:10:import com.patrykmichalik.opto.core.PreferenceManager
lawnchair/src/app/lawnchair/predictions/LawnchairPredictionManager.kt:14: * [PreferenceManager] for prediction-related data.
lawnchair/src/app/lawnchair/predictions/LawnchairPredictionManager.kt:21:) : PreferenceManager {
lawnchair/src/app/lawnchair/predictions/LawnchairModelDelegate.kt:7:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/predictions/LawnchairModelDelegate.kt:38:    private val prefs2: PreferenceManager2 by lazy { PreferenceManager2.getInstance(context) }
lawnchair/src/app/lawnchair/predictions/LawnchairPredictionEngine.kt:14:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/predictions/LawnchairPredictionEngine.kt:32:    private val prefs2: PreferenceManager2 by lazy { PreferenceManager2.getInstance(context) }
lawnchair/src/app/lawnchair/icons/LawnchairIconProvider.kt:34:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/icons/LawnchairIconProvider.kt:58:    private val prefs = PreferenceManager.getInstance(context)
lawnchair/src/app/lawnchair/icons/LawnchairIconProvider.kt:314:        private val iconPackPref = PreferenceManager.getInstance(context).iconPackPackage
lawnchair/src/app/lawnchair/icons/LawnchairIconProvider.kt:315:        private val themedIconPackPref = PreferenceManager.getInstance(context).themedIconPackPackage
lawnchair/src/app/lawnchair/icons/LawnchairThemeManager.kt:8:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/icons/LawnchairThemeManager.kt:9:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/icons/LawnchairThemeManager.kt:37:    private val prefs2: PreferenceManager2,
lawnchair/src/app/lawnchair/icons/LawnchairThemeManager.kt:38:    private val prefs1: PreferenceManager,
lawnchair/src/app/lawnchair/icons/ThemeManagerModule.kt:4:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/icons/ThemeManagerModule.kt:5:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/icons/ThemeManagerModule.kt:27:        prefs2: PreferenceManager2,
lawnchair/src/app/lawnchair/icons/ThemeManagerModule.kt:28:        prefs1: PreferenceManager,
lawnchair/src/app/lawnchair/icons/shape/IconShapeManager.kt:24:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/icons/shape/IconShapeManager.kt:60:            val prefs = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:4:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:5:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:23:    private val prefs = PreferenceManager.getInstance(context)
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:24:    private val preferenceManager2 = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:82:        constructor(prefs: PreferenceManager) : this(
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:114:            prefs: PreferenceManager,
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:115:            prefs2: PreferenceManager2,
lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt:207:            prefs2: PreferenceManager2,
lawnchair/src/app/lawnchair/LawnchairApp.kt:43:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/LawnchairApp.kt:118:        val prefs = PreferenceManager.INSTANCE.get(this)
lawnchair/src/app/lawnchair/FeedBridge.kt:27:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/FeedBridge.kt:40:    private val prefs by lazy { PreferenceManager.getInstance(context) }
lawnchair/src/app/lawnchair/SearchBarStateHandler.kt:8:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/SearchBarStateHandler.kt:22:    private val preferenceManager2 = PreferenceManager2.getInstance(launcher)
lawnchair/src/app/lawnchair/qsb/providers/StartpageEU.kt:4:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/qsb/providers/StartpageEU.kt:20:        val prefs = PreferenceManager.getInstance(launcher)
lawnchair/src/app/lawnchair/qsb/providers/QsbSearchProvider.kt:10:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/qsb/providers/QsbSearchProvider.kt:35:        val prefs = PreferenceManager2.getInstance(launcher)
lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt:23:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt:51:    private lateinit var preferenceManager2: PreferenceManager2
lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt:59:        preferenceManager2 = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/qsb/LawnQsbLayout.kt:264:            preferenceManager: PreferenceManager2,
lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt:46:class PreferenceManager @Inject constructor(
lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt:48:) : BasePreferenceManager(context),
lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt:241:        val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getPreferenceManager)
lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt:251:fun preferenceManager() = PreferenceManager.getInstance(LocalContext.current)
lawnchair/src/app/lawnchair/preferences/PreferenceAdapter.kt:87:fun BasePreferenceManager.IdpIntPref.getAdapter(): PreferenceAdapter<Int> {
lawnchair/src/app/lawnchair/preferences/BasePreferenceManager.kt:28:sealed class BasePreferenceManager(private val context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
lawnchair/src/app/lawnchair/theme/color/tokens/AllAppsTabColors.kt:5:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/theme/color/tokens/AllAppsTabColors.kt:18:        val prefs2 = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/theme/color/tokens/ColorTokenUtils.kt:4:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/theme/color/tokens/ColorTokenUtils.kt:10:    crossinline transform: ColorToken.(PreferenceManager) -> ColorToken,
lawnchair/src/app/lawnchair/theme/color/tokens/ColorTokenUtils.kt:12:    val prefs = PreferenceManager.getInstance(context)
lawnchair/src/app/lawnchair/theme/ThemeProvider.kt:11:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/theme/ThemeProvider.kt:40:    private val preferenceManager2 = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/override/CustomizeDialog.kt:45:import app.lawnchair.ui.preferences.components.controls.SwitchPreference
lawnchair/src/app/lawnchair/override/CustomizeDialog.kt:194:            SwitchPreference(
lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt:12:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt:13:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt:42:    private val prefs2 = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt:43:    private val prefs = PreferenceManager.getInstance(context)
lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt:40:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt:41:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt:116:    private val prefs = PreferenceManager.getInstance(launcher)
lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt:117:    private val prefs2 = PreferenceManager2.getInstance(launcher)
lawnchair/src/app/lawnchair/ui/preferences/Preferences.kt:105:                PreferenceScreen(
lawnchair/src/app/lawnchair/ui/preferences/Preferences.kt:127:private fun PreferenceScreen(
lawnchair/src/app/lawnchair/ui/preferences/components/colorpreference/ColorPreferenceModelList.kt:4:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/ui/preferences/components/colorpreference/ColorPreferenceModelList.kt:19:        val prefs = PreferenceManager2.getInstance(context)
lawnchair/src/app/lawnchair/ui/preferences/components/QuickActionsPreferences.kt:11:import app.lawnchair.ui.preferences.components.reorderable.ReorderableSwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/QuickActionsPreferences.kt:66:        ReorderableSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/AppDrawerHapticFeedbackPreference.kt:13:import app.lawnchair.ui.preferences.components.controls.SwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/AppDrawerHapticFeedbackPreference.kt:23:        SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/reorderable/ReorderablePreferenceDefaults.kt:66:fun ReorderableSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/HomeLayoutPreferences.kt:40:import app.lawnchair.ui.preferences.components.controls.SwitchPreferenceWithPreview
lawnchair/src/app/lawnchair/ui/preferences/components/HomeLayoutPreferences.kt:111:    SwitchPreferenceWithPreview(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:35:import app.lawnchair.ui.preferences.components.controls.MainSwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:37:import app.lawnchair.ui.preferences.components.controls.SwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:38:import app.lawnchair.ui.preferences.components.controls.TwoTargetSwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:89:    MainSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:166:        SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:172:        TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:224:        SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:231:        TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:320:        TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:331:        SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:425:private fun TwoTargetSwitchPreferencePreview() {
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:429:            TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/FileSearchProvider.kt:436:            TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:10:import app.lawnchair.preferences.PreferenceManager
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:14:import app.lawnchair.preferences2.PreferenceManager2
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:22:import app.lawnchair.ui.preferences.components.controls.MainSwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:23:import app.lawnchair.ui.preferences.components.controls.SwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:24:import app.lawnchair.ui.preferences.components.controls.TwoTargetSwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:44:    MainSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:53:            SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:60:            SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:74:                TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:102:private fun ASISearchSettings(prefs: PreferenceManager) {
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:103:    SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:107:    SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:111:    SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:115:    SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:151:    prefs: PreferenceManager,
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:152:    prefs2: PreferenceManager2,
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:159:    TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:176:    TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:188:    TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:198:    TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:205:    TwoTargetSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt:212:    SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/DockSearchPreferences.kt:49:import app.lawnchair.ui.preferences.components.controls.SwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/DockSearchPreferences.kt:113:                            SwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/SearchProviderPreference.kt:30:import app.lawnchair.ui.preferences.components.controls.MainSwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/SearchProviderPreference.kt:32:import app.lawnchair.ui.preferences.components.controls.SwitchPreference
lawnchair/src/app/lawnchair/ui/preferences/components/search/SearchProviderPreference.kt:69:fun SearchProviderPreferenceScreen(
lawnchair/src/app/lawnchair/ui/preferences/components/search/SearchProviderPreference.kt:105:    MainSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/SearchProviderPreference.kt:157:    MainSwitchPreference(
lawnchair/src/app/lawnchair/ui/preferences/components/search/SearchProviderPreference.kt:188:                    SwitchPreference(
## Security
./flags/src/com/android/systemui/Flags.java:769:    public static boolean dreamBiometricPromptFixes() {
./flags/src/com/android/systemui/Flags.java:771:        return FEATURE_FLAGS.dreamBiometricPromptFixes();
./flags/src/com/android/systemui/CustomFeatureFlags.java:307:    public boolean dreamBiometricPromptFixes() {
./flags/src/com/android/systemui/CustomFeatureFlags.java:309:            FeatureFlags::dreamBiometricPromptFixes);
./flags/src/com/android/systemui/FeatureFlagsImpl.java:295:    public boolean dreamBiometricPromptFixes() {
./flags/src/com/android/systemui/FeatureFlags.java:150:    boolean dreamBiometricPromptFixes();
./systemUI/shared/biometrics/src/com/android/systemui/biometrics/shared/model/BiometricUserInfo.kt:20: * Metadata about the current user BiometricPrompt is being shown to.
./systemUI/plugin/src/com/android/systemui/plugins/AuthContextPlugin.kt:29: * sensitive surfaces like lock screen and BiometricPrompt.
./systemUI/plugin/src/com/android/systemui/plugins/AuthContextPlugin.kt:48:     * example, [SensitiveSurface.BiometricPrompt.isCredential] will change if the user falls back
./systemUI/plugin/src/com/android/systemui/plugins/AuthContextPlugin.kt:71:        /** Information about the BiometricPrompt that is being shown to the user. */
./systemUI/plugin/src/com/android/systemui/plugins/AuthContextPlugin.kt:72:        data class BiometricPrompt(val view: View? = null, val isCredential: Boolean = false) :
