package /*++{0}++*//*--*/jp.ats.dexter.plugin/*--*/;/*--*///自動フォーマット禁止/*--*/

/*++{1}++*//*==JspodImportsPart==*/
import static jp.ats.substrate.U.care;

import java.util.LinkedHashSet;
import java.util.Map;

import jp.ats.dexter.DexterManager;
import jp.ats.dexter.Jspod;
import jp.ats.dexter.JspodHelper;
import jp.ats.dexter.JspodToken;
import jp.ats.substrate.util.MapMap;
import jp.ats.substrate.util.Sanitizer;
/*==JspodImportsPart==*/

/*++{2}++*/
public class /*++{3}++*//*--*/Template/*--*/
extends /*++{4}++*//*--*/Object/*--*/
implements Jspod /*++'++*/{/*++'++*/

/*++{5}++*//*==JspodPropertiesPart==*/
	/*++{0}++*/
	private /*++{2}++*//*--*/String/*--*/ /*++{1}++*//*--*/name/*--*/;
/*==JspodPropertiesPart==*/

/*++{6}++*//*==JspodMapPropertiesPart==*/
	private Map<String, /*++{1}++*//*--*/String/*--*/> /*++{0}++*//*--*/nameMap/*--*/;
/*==JspodMapPropertiesPart==*/

	private final JspodToken token;

	private final LinkedHashSet<String> indices = new LinkedHashSet<>();

	public /*++{3}++*//*--*/Template/*--*/() /*++'++*/{/*++'++*/
		token = DexterManager.generateToken();
	/*++'++*/}/*++'++*/

	public /*++{3}++*//*--*/Template/*--*/(Map<String, String[]> parameterMap) /*++'++*/{/*++'++*/
		token = DexterManager.generateToken(parameterMap);
		apply(parameterMap);
	/*++'++*/}/*++'++*/

	@Override
	public void apply(Map<String, String[]> parameterMap) /*++'++*/{/*++'++*/
		MapMap<String, String, String> mapMap = JspodHelper.convert(parameterMap, indices);
/*++{7}++*//*==JspodApplyPart==*//*++{0}++*//*--*/nameMap/*--*/
			= JspodHelper./*++{3}++*//*--*/toString/*--*/(mapMap.get(/*++{1}++*//*--*/"nameMap"/*--*/));
		if (/*++{0}++*//*--*/nameMap/*--*/ != null && /*++{0}++*//*--*/nameMap/*--*/.size() == 1)
				/*++{2}++*//*--*/name/*--*/ = /*++{0}++*//*--*/nameMap/*--*/.get("0");
/*==JspodApplyPart==*//*++'++*/}/*++'++*/

	@Override
	public JspodToken token() /*++'++*/{/*++'++*/
		return token;
	/*++'++*/}/*++'++*/

	public String tokenName() /*++'++*/{/*++'++*/
		return DexterManager.getTokenName();
	/*++'++*/}/*++'++*/

	public String tokenValue() /*++'++*/{/*++'++*/
		return token.toString();
	/*++'++*/}/*++'++*/

	public Iterable<String> indicies() /*++'++*/{/*++'++*/
		return new LinkedHashSet<String>(indices);
	/*++'++*/}/*++'++*/

/*++{8}++*//*==JspodSetterGetterPart==*/
	public void set/*++{0}++*/(/*++{3}++*//*--*/String/*--*/ value) /*++'++*/{/*++'++*/
		/*++{1}++*//*--*/name/*--*/ = value;
	/*++'++*/}/*++'++*/

	public void set/*++{0}++*/(int index, /*++{3}++*//*--*/String/*--*/ value) /*++'++*/{/*++'++*/
		String key = String.valueOf(index);
		/*++{2}++*//*--*/nameMap/*--*/.put(key, value);
		indices.add(key);
	/*++'++*/}/*++'++*/

	public void set/*++{0}++*/(String key, /*++{3}++*//*--*/String/*--*/ value) /*++'++*/{/*++'++*/
		/*++{2}++*//*--*/nameMap/*--*/.put(key, value);
		indices.add(key);
	/*++'++*/}/*++'++*/

	public /*++{3}++*//*--*/String/*--*/ get/*++{0}++*/() /*++'++*/{/*++'++*/
		return /*++{1}++*//*--*/name/*--*/;
	/*++'++*/}/*++'++*/

	public /*++{3}++*//*--*/String/*--*/ get/*++{0}++*/(int index) /*++'++*/{/*++'++*/
		return /*++{2}++*//*--*/nameMap/*--*/.get(String.valueOf(index));
	/*++'++*/}/*++'++*/

	public /*++{3}++*//*--*/String/*--*/ get/*++{0}++*/(String key) /*++'++*/{/*++'++*/
		return /*++{2}++*//*--*/nameMap/*--*/.get(key);
	/*++'++*/}/*++'++*/

	public String /*--*/getSafely/*--*//*++get{0}Safely++*/() /*++'++*/{/*++'++*/
		return care(Sanitizer.sanitize(JspodHelper.stringOf(get/*++{0}++*/())));
	/*++'++*/}/*++'++*/

	public String /*--*/getSafely/*--*//*++get{0}Safely++*/(int index) /*++'++*/{/*++'++*/
		return care(Sanitizer.sanitize(JspodHelper.stringOf(get/*++{0}++*/(index))));
	/*++'++*/}/*++'++*/

	public String /*--*/getSafely/*--*//*++get{0}Safely++*/(String key) /*++'++*/{/*++'++*/
		return care(Sanitizer.sanitize(JspodHelper.stringOf(get/*++{0}++*/(key))));
	/*++'++*/}/*++'++*//*==JspodSetterGetterPart==*/
/*++'++*/}/*++'++*/
