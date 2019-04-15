package hearsilent.discreteslider.libs;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class Utils {

	public static DisplayMetrics getDisplayMetrics(Context context) {
		Resources resources = context.getResources();
		return resources.getDisplayMetrics();
	}

	/**
	 * This method converts dp unit to equivalent pixels, depending on device
	 * density.
	 *
	 * @param dp      A value in dp (density independent pixels) unit. Which we need
	 *                to convert into pixels
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent px equivalent to dp depending on
	 * device density
	 */
	public static float convertDpToPixel(float dp, Context context) {
		return dp * (getDisplayMetrics(context).densityDpi / 160f);
	}

	/**
	 * This method converts sp unit to equivalent pixels, depending on device
	 * density.
	 *
	 * @param sp      A value in sp (scale-independent pixels) unit. Which we need
	 *                to convert into pixels
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent px equivalent to sp depending on
	 * device density
	 */
	public static int convertSpToPixel(float sp, Context context) {
		return (int) TypedValue
				.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getDisplayMetrics(context));
	}

	/**
	 * This method converts device specific pixels to density independent
	 * pixels.
	 *
	 * @param px      A value in px (pixels) unit. Which we need to convert into dp
	 * @param context Context to get resources and device specific display metrics
	 * @return A float value to represent dp equivalent to px value
	 */
	private static float convertPixelsToDp(float px, Context context) {
		return px / (getDisplayMetrics(context).densityDpi / 160f);
	}

}
