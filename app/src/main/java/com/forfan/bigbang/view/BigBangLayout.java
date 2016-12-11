package com.forfan.bigbang.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import com.forfan.bigbang.R;
import com.forfan.bigbang.util.ConstantUtil;
import com.forfan.bigbang.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

public class BigBangLayout extends ViewGroup implements BigBangHeader.ActionListener, NestedScrollingChild {

    public static final String ENTER = "_Enter_";
    public static final String ENTER_SYMBOL = "\n";
    public static final String TAB = "_Tab_";

    private static final int DEFAULT_TEXT_SIZE = 14;//sp
    private static final int DEFAULT_TEXT_COLOR_RES = R.color.bigbang_item_text;
    private static final int DEFAULT_TEXT_BG_RES = R.drawable.item_background;
    private static final int DEFAULT_SECTION_TEXT_BG_RES = R.drawable.item_background_section;
    private int mLineSpace;
    private int mItemSpace;
    private int mTextColorRes;
    private int mSectionTextBgRes;
    private int mTextSize;
    private int mTextPadding = (int) ViewUtil.dp2px(ConstantUtil.DEFAULT_ITEM_PADDING);
    private int mTextBgRes;

    private Item mTargetItem;
    private List<Line> mLines;
    private List<Integer> mSectionIndex;
    private int mActionBarTopHeight;
    private int mActionBarBottomHeight;
    private BigBangHeader mHeader;

    private boolean showAnimation = false;
    private Paint dragPaint;
    private boolean dragMode = false;
    private Item dragItem;

    private ColorStateList mColorStateList;
    private boolean stickHeader = false;
    private int mOriginActionBarTopHeight;

    private AnimatorListenerAdapter mActionBarAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            if (findFirstSelectedLine() == null) {
                mHeader.setVisibility(View.GONE);
            } else {
                requestLayout();
            }
        }
    };
    private ActionListener mActionListener;
    private int mScaledTouchSlop;
    private float mDownX;
    private boolean mDisallowedParentIntercept;


    private boolean showSymbol = false;
    private boolean showSection = false;

    public BigBangLayout(Context context) {
        super(context);
    }

    public BigBangLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigBangLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BigBangLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(attrs);
    }


    private void initView(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.BigBangLayout);
            mItemSpace = typedArray.getDimensionPixelSize(R.styleable.BigBangLayout_itemSpace, getResources().getDimensionPixelSize(R.dimen.big_bang_default_item_space));
            mLineSpace = typedArray.getDimensionPixelSize(R.styleable.BigBangLayout_lineSpace, getResources().getDimensionPixelSize(R.dimen.big_bang_default_line_space));

            mTextColorRes = typedArray.getResourceId(R.styleable.BigBangLayout_textColor, DEFAULT_TEXT_COLOR_RES);
            mTextSize = (int) ViewUtil.px2sp(typedArray.getDimension(R.styleable.BigBangLayout_textSize, ViewUtil.sp2px(DEFAULT_TEXT_SIZE)));
            mTextBgRes = typedArray.getResourceId(R.styleable.BigBangLayout_textBackground, DEFAULT_TEXT_BG_RES);
            mSectionTextBgRes = typedArray.getResourceId(R.styleable.BigBangLayout_sectionTextBackground, DEFAULT_SECTION_TEXT_BG_RES);
            typedArray.recycle();
            mActionBarBottomHeight = mLineSpace;
            mActionBarTopHeight = getResources().getDimensionPixelSize(R.dimen.big_bang_action_bar_height);
            mOriginActionBarTopHeight=mActionBarTopHeight;
        }

        // TODO 暂时放到这里
        mHeader = new BigBangHeader(getContext());
        mHeader.setVisibility(View.GONE);
        mHeader.setActionListener(this);


        dragPaint = new Paint();
//        dragPaint.setAlpha(100);
        dragPaint.setAntiAlias(true);

        addView(mHeader, 0);

        setClipChildren(false);

        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();


        setOnDragListener(new OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                int eventType = event.getAction();
//                View view= (View) event.getLocalState();
//                if (eventType==DragEvent.ACTION_DRAG_ENDED){
//                    return true;
//                }
//                String txt=event.getClipDescription().getLabel().toString();
                try {
                    if (dragItem == null || !((TextView) dragItem.view).getText().equals(event.getClipDescription().getLabel())) {
                        return false;
                    }
                } catch (Throwable e) {
                    return false;
                }

                Item item = findItemByPoint(x, y);
                Log.e("findItemIndexByPoint", "item=" + item + "," + (item != null ? item.index : -1));
                if (item == null) {
                    item = findItemIndexByPoint(x, y);
                    if (item == null) {
                        if (eventType == DragEvent.ACTION_DRAG_ENDED) {
                            removeView(dragItem.view);
                            addView(dragItem.view, dragItem.index);
                            mTargetItem = null;
                        }
                        return true;
                    }
                }
                if (mTargetItem != null && mTargetItem.view == item.view) {
                    return true;
                } else {
                    removeView(dragItem.view);
                    addView(dragItem.view, item.index);
                    dragItem.index = item.index;
                    mTargetItem = item;
                }
                return true;
            }
        });
        mSectionIndex = new ArrayList<>();
    }


    public void setStickHeader(boolean stickHeader) {
        this.stickHeader = stickHeader;
        mHeader.setStickHeader(stickHeader);
        if (stickHeader){
            mActionBarTopHeight=0;
        }else {
            mActionBarTopHeight=mOriginActionBarTopHeight;
        }
    }

    public int getLineSpace() {
        return mLineSpace;
    }

    public void setLineSpace(int mLineSpace) {
        this.mLineSpace = mLineSpace;
        requestLayout();
    }

    public int getItemSpace() {
        return mItemSpace;
    }

    public void setItemSpace(int mItemSpace) {
        this.mItemSpace = mItemSpace;
        requestLayout();
    }

    public ColorStateList getTextColorStateList() {
        return mColorStateList;
    }

    public void setTextColorStateList(ColorStateList colorStateList) {
        // TODO: 2016/11/12
        this.mColorStateList = colorStateList;
    }

    public int getTextSize() {
        return mTextSize;
    }

    public void setTextSize(int mTextSize) {
        this.mTextSize = mTextSize;
        if (mLines != null) {
            for (Line line : mLines) {
                List<Item> items = line.getItems();
                for (Item item : items) {
                    ((TextView) item.view).setTextSize(mTextSize);
                }
            }
        }
    }

    public void setTextPadding(int padding){
        mTextPadding=padding;
        if (mLines != null) {
            for (Line line : mLines) {
                List<Item> items = line.getItems();
                for (Item item : items) {
                    ((TextView) item.view).setPadding(mTextPadding,0,mTextPadding,0);
                }
            }
        }
    }

    public int getTextBgRes() {
        return mTextBgRes;
    }

    public void setTextBgRes(int mTextBgRes) {
        // TODO: 2016/11/12
        this.mTextBgRes = mTextBgRes;
    }

    public void addTextItem(String text) {
        if (TextUtils.isEmpty(text) || text.equals(" ")) {
            return;
        }
        if (text.contains(TAB)) {
            return;
        }
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setBackgroundResource(mTextBgRes);
        if (mColorStateList == null) {
            view.setTextColor(ContextCompat.getColorStateList(getContext(), mTextColorRes));
        } else {
            view.setTextColor(mColorStateList);
        }
        view.setTextSize(mTextSize);
        view.setPadding(mTextPadding,0,mTextPadding,0);
        view.setGravity(Gravity.CENTER);
        addView(view);
    }

    public void reset() {
        showAnimation = false;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (mHeader == child) {
                mHeader.setVisibility(View.GONE);
                continue;
            }
            removeView(child);
        }
    }

    public boolean isShowSymbol() {
        return showSymbol;
    }

    public void setShowSymbol(boolean showSymbol) {
        this.showSymbol = showSymbol;
        requestLayout();
    }

    public boolean isShowSection() {
        return showSection;
    }

    public void setShowSection(boolean showSection) {
        this.showSection = showSection;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int contentWidthSize = widthSize - mHeader.getContentPadding();
        int heightSize = 0;

        int childCount = getChildCount();

        int measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        mLines = new ArrayList<>();
        mSectionIndex.clear();
        Line currentLine = null;
        int currentLineWidth = contentWidthSize;
        boolean isEnter = true;
        for (int i = 0; i < childCount; i++) {

            View v = getChildAt(i);

            if (mHeader == v) {
                continue;
            }

            TextView child = (TextView) v;
            String content = child.getText().toString();

            child.setVisibility(VISIBLE);
            if (!showSymbol) {
                if (content.matches("[,\\./:\"\\\\\\[\\]\\|`~!@#\\$%\\^&\\*\\(\\)_\\+=<->\\?;'，。、；：‘’“”【】《》？\\{\\}！￥…（）—=]")) {
                    child.setVisibility(GONE);
                    continue;
                }
            }
            if (content.contains(ENTER) || content.equals(ENTER_SYMBOL)) {
                child.setVisibility(GONE);
                mSectionIndex.add(i);
                isEnter = true;
                continue;
            }
            child.measure(measureSpec, measureSpec);

            if (currentLineWidth > 0) {
                currentLineWidth += mItemSpace;
            }
            currentLineWidth += child.getMeasuredWidth();
            if (mLines.size() == 0 || currentLineWidth > contentWidthSize || (isEnter && showSection)) {
                heightSize += child.getMeasuredHeight();
                currentLineWidth = child.getMeasuredWidth();
                currentLine = new Line(mLines.size());
                mLines.add(currentLine);
            }
            Item item = new Item(currentLine);
            item.view = child;
            item.index = i;
            item.width = child.getMeasuredWidth();
            item.height = child.getMeasuredHeight();
            if (currentLine.getItems() == null && (isEnter && showSection)) {
                child.setBackgroundResource(mSectionTextBgRes);
            } else {
                child.setBackgroundResource(mTextBgRes);
            }
            currentLine.addItem(item);
            isEnter = false;
        }

        Line firstSelectedLine = findFirstSelectedLine();
        Line lastSelectedLine = findLastSelectedLine();
        if (firstSelectedLine != null && lastSelectedLine != null) {
            int selectedLineHeight = (lastSelectedLine.maxIndex - firstSelectedLine.maxIndex + 1) * (firstSelectedLine.getHeight() + mLineSpace);
            mHeader.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(selectedLineHeight, MeasureSpec.UNSPECIFIED));
        }

        int size = heightSize + getPaddingTop() + getPaddingBottom() + (mLines.size()) * mLineSpace +mActionBarTopHeight + mActionBarBottomHeight;
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int top;
        int left;
        int offsetTop;

        Line lastSelectedLine = findLastSelectedLine();
        Line firstSelectedLine = findFirstSelectedLine();

        for (int i = 0; i < mLines.size(); i++) {
            Line line = mLines.get(i);
            List<Item> items = line.getItems();
            left = getPaddingLeft() + mHeader.getContentPadding();

            if (firstSelectedLine != null && firstSelectedLine.maxIndex > line.maxIndex) {
                //如果在第一个被选中行以前，则需要上移mActionBarTopHeight的距离
                offsetTop = -mActionBarTopHeight;
            } else if (lastSelectedLine != null && lastSelectedLine.maxIndex < line.maxIndex) {
                //如果在最后一个被选中行以后，则需要下移mActionBarBottomHeight的距离
                offsetTop = mActionBarBottomHeight;
            } else {
                offsetTop = 0;
            }

            for (int j = 0; j < items.size(); j++) {
                Item item = items.get(j);
                top = getPaddingTop() + i * (item.height + mLineSpace) + offsetTop + mActionBarTopHeight;
                View child = item.view;
                int oldTop = child.getTop();
                child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
                if (showAnimation && oldTop != top) {
                    int translationY = oldTop - top;
                    child.setTranslationY(translationY);
                    child.animate().translationYBy(-translationY).setDuration(200).start();
                }
                left += child.getMeasuredWidth() + mItemSpace;
            }
        }
        if (!stickHeader) {
            if (firstSelectedLine != null && lastSelectedLine != null) {
                mHeader.setVisibility(View.VISIBLE);
                mHeader.setAlpha(1);
                int oldTop = mHeader.getTop();
                int actionBarTop = firstSelectedLine.maxIndex * (firstSelectedLine.getHeight() + mLineSpace) + getPaddingTop();
                mHeader.layout(getPaddingLeft(), actionBarTop, getPaddingLeft() + mHeader.getMeasuredWidth(), actionBarTop + mHeader.getMeasuredHeight());
                if (oldTop != actionBarTop) {
                    int translationY = oldTop - actionBarTop;
                    mHeader.setTranslationY(translationY);
                    mHeader.animate().translationYBy(-translationY).setDuration(200).start();
                }
            } else {
                if (mHeader.getVisibility() == View.VISIBLE && !dragMode) {
                    mHeader.animate().alpha(0).setDuration(200).setListener(mActionBarAnimationListener).start();
                }
            }
        }else {
            mHeader.setVisibility(GONE);
        }
    }

    private Line findLastSelectedLine() {
        Line result = null;
        for (Line line : mLines) {
            if (line.hasSelected()) {
                result = line;
            }
        }
        return result;
    }

    private Line findFirstSelectedLine() {
        for (Line line : mLines) {
            if (line.hasSelected()) {
                return line;
            }
        }
        return null;
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return true;
//    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e("onTouchEvent", "onTouchEvent:" + event);
        int actionMasked = MotionEventCompat.getActionMasked(event);
        if (dragMode) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (actionMasked) {
                case MotionEvent.ACTION_DOWN:
                    showAnimation = true;
                    mDownX = x;
                    mDisallowedParentIntercept = false;
                    Item item = findItemByPoint(x, y);
                    if (item != null) {
                        dragItem = new Item(item);
                        ClipData clipData = ClipData.newPlainText(((TextView) dragItem.view).getText(), ((TextView) dragItem.view).getText());
                        View.DragShadowBuilder myShadow = new DragShadowBuilder(dragItem.view);
                        dragItem.view.startDrag(clipData, myShadow, null, 0);
                        removeView(dragItem.view);
                    } else {
                        dragItem = null;
                    }
                case MotionEvent.ACTION_MOVE:
                    if (!mDisallowedParentIntercept && Math.abs(x - mDownX) > mScaledTouchSlop) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        mDisallowedParentIntercept = true;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
//                    dragItem = null;
                    if (mTargetItem == null && dragItem != null) {
                        removeView(dragItem.view);
                        addView(dragItem.view, dragItem.index);
                    }
                    mTargetItem = null;
                    requestLayout();
                    invalidate();
                    if (mDisallowedParentIntercept) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    break;
            }
        } else {
            int x = (int) event.getX();
            switch (actionMasked) {
                case MotionEvent.ACTION_DOWN:
                    showAnimation = true;
                    mDownX = x;
                    mDisallowedParentIntercept = false;
                case MotionEvent.ACTION_MOVE:
                    if (!mDisallowedParentIntercept && Math.abs(x - mDownX) > mScaledTouchSlop) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        mDisallowedParentIntercept = true;
                    }
                    Item item = findItemByPoint(x, (int) event.getY());
                    if (mTargetItem != item) {
                        mTargetItem = item;
                        if (item != null) {
                            item.setSelected(!item.isSelected());
                            ItemState state = new ItemState();
                            state.item = item;
                            state.isSelected = item.isSelected();
                            if (mItemState == null) {
                                mItemState = state;
                            } else {
                                state.next = mItemState;
                                mItemState = state;
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    if (mItemState != null) {
                        ItemState state = mItemState;
                        while (state != null) {
                            state.item.setSelected(!state.isSelected);
                            state = state.next;
                        }
                    }
                case MotionEvent.ACTION_UP:
                    requestLayout();
                    invalidate();
                    mTargetItem = null;
                    if (mDisallowedParentIntercept) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    mItemState = null;
                    String selected = makeSelectedText();
                    if (!TextUtils.isEmpty(selected)) {
                        if (mActionListener != null) {
                            mActionListener.onSelected(selected);
                        }
                    }
                    break;
            }
        }
        return true;
    }

    ItemState mItemState;

    public void setBackgroundColorAlpha(int value) {
        value = (int) ((value / 100.0f) * 255);
        setBackgroundColor(Color.argb(value, 00, 00, 00));
    }

    class ItemState {
        Item item;
        boolean isSelected;
        ItemState next;
    }

    private Item findItemByPoint(int x, int y) {
        for (Line line : mLines) {
            List<Item> items = line.getItems();
            for (Item item : items) {
                if (item.getRect().contains(x, y)) {
                    return item;
                }
            }
        }
        return null;
    }

    private Item findItemIndexByPoint(int x, int y) {
        int height = 0;
        int lineNum = 0;
        int length = mLines.size();
        if (y > mLines.get(0).getHeight() / 2 + mActionBarTopHeight && y < getHeight() - mLines.get(0).getHeight() - mLineSpace) {
            // TODO: 2016/10/27 调整效果
            return null;
        }
//        for (int i=0;i<length;i++) {
//            if (y-mLines.get(i).getHeight()/2<0 ){
//                lingNum=i;
//                break;
//            }
//            if (height <= y-mLines.get(i).getHeight()/2+mLineSpace/2 && height+mLines.get(i).getHeight()+mLineSpace>= y-mLines.get(i).getHeight()/2+mLineSpace/2 ){
//                lingNum=i;
//                break;
//            }
//            height+=mLines.get(i).getHeight()+mLineSpace;
//            lingNum=i;
//        }
        if (mLines.get(0).hasSelected() && y <= mActionBarTopHeight) {
            lineNum = 0;
        } else if (!mLines.get(0).hasSelected() && y <= mLines.get(0).getHeight() / 2 + mActionBarTopHeight) {
            lineNum = 0;
        } else if (y >= getHeight() - mLines.get(0).getHeight() - mLineSpace) {
            lineNum = mLines.size() - 1;
        } else {
            return null;
        }
        length = mLines.get(lineNum).getItems().size();
        List<Item> items = mLines.get(lineNum).getItems();
        height = 0;
        for (int i = 0; i < length; i++) {
            if (height <= x - items.get(i).view.getMeasuredWidth() / 2 + mItemSpace / 2 && height >= x - items.get(i).view.getMeasuredWidth() + mItemSpace / 2) {
                return items.get(i);
            }
            height += items.get(i).view.getMeasuredWidth() + mItemSpace;
        }
        if (height <= x - items.get(items.size() - 1).view.getMeasuredWidth() / 2) {
            return items.get(items.size() - 1);
        }
        return null;
    }


    private View findChildByPoint(int x, int y) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            Rect rect = new Rect();
            child.getHitRect(rect);
            if (rect.contains(x, y)) {
                return child;
            }
        }
        return null;
    }

    private String makeSelectedText() {
        StringBuilder builder = new StringBuilder();
        int length = mLines.size();
        for (int i = 0; i < length; i++) {
            Line line = mLines.get(i);
            boolean containEnter = false;
            if (i < length - 1) {
                Line nextLine = mLines.get(i + 1);
                int thisLineLastIndex = line.getItems().get(line.getItems().size() - 1).index;
                int nextLineFirstIndex = nextLine.getItems().get(nextLine.getItems().size() - 1).index;
                for (int j = thisLineLastIndex; j < nextLineFirstIndex; j++) {
                    if (mSectionIndex.contains(j)) {
                        containEnter = true;
                    }
                }
            }
            builder.append(line.getSelectedText());
            if (containEnter&&showSection) {
                builder.append("\n");
            }
        }
        return builder.toString().replaceAll("[\\n]+","\n").trim();
    }

    @Override
    public void onSearch() {
        if (mActionListener != null) {
            String text = makeSelectedText();
            mActionListener.onSearch(text);
        }
    }

    @Override
    public void onShare() {
        if (mActionListener != null) {
            String text = makeSelectedText();
            mActionListener.onShare(text);
        }
    }

    @Override
    public void onCopy() {
        if (mActionListener != null) {
            String text = makeSelectedText();
            mActionListener.onCopy(text);
        }
    }

    public void onDrag() {
        dragMode = !dragMode;
        if (mActionListener != null) {
            mActionListener.onDrag();
        }
    }

    @Override
    public void onTrans() {
        if (mActionListener != null) {
            String text = makeSelectedText();
            mActionListener.onTrans(text);
        }
    }

    public void onSelectOther() {
        for (Line line : mLines) {
            List<Item> items = line.getItems();
            for (Item item : items) {
                item.setSelected(!item.isSelected());
            }
        }
        requestLayout();
    }

    public void setActionListener(ActionListener actionListener) {
        mActionListener = actionListener;
    }

    class Line {
        int maxIndex;
        List<Item> items;

        public Line(int maxIndex) {
            this.maxIndex = maxIndex;
        }

        void addItem(Item item) {
            if (items == null) {
                items = new ArrayList<>();
            }
            items.add(item);
        }

        List<Item> getItems() {
            return items;
        }

        boolean hasSelected() {
            for (Item item : items) {
                if (item.isSelected()) {
                    return true;
                }
            }
            return false;
        }

        int getHeight() {
            List<Item> items = getItems();
            if (items != null && items.size() > 0) {
                return items.get(0).view.getMeasuredHeight();
            }
            return 0;
        }

        String getSelectedText() {
            StringBuilder builder = new StringBuilder();
            List<Item> items = getItems();
            if (items != null && items.size() > 0) {
                for (Item item : items) {
                    if (item.isSelected()) {
                        String txt = item.getText().toString();
                        builder.append(txt);
                        if (txt.matches("[a-zA-Z0-9]*")) {
                            builder.append(" ");
                        }
                    }
                }
            }
            return builder.toString();
        }

    }

    static class Item {
        Line line;
        int index;
        int height;
        int width;
        View view;

        public Item(Line line) {
            this.line = line;
        }

        public Item(Item item) {
            if (item != null) {
                this.line = item.line;
                this.index = item.index;
                this.height = item.height;
                this.width = item.width;
                this.view = item.view;
            }
        }

        Rect getRect() {
            Rect rect = new Rect();
            view.getHitRect(rect);
            return rect;
        }

        boolean isSelected() {
            return view.isSelected();
        }

        void setSelected(boolean selected) {
            view.setSelected(selected);
        }

        CharSequence getText() {
            return ((TextView) view).getText();
        }
    }

    /**
     * Action Listener
     */
    public interface ActionListener {
        void onSelected(String text);

        void onSearch(String text);

        void onShare(String text);

        void onCopy(String text);

        void onTrans(String text);

        void onDrag();

    }

}
