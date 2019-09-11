package carnival.util



/** */
class StringUtils {

    /** */
    static public String toCapitalCase(String text) {
        toCamelCase(text, true)
    }

    /** */
    static public String toCamelCase(String text, boolean capitalized = false) {
        text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
        return capitalized ? capitalize(text) : text
    }
     
    /** 
     * stringsLikeThis to strings_like_this
     *
     */
    static public String toSnakeCase(String text) {
        text.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
    }

    /** 
     * stringsLikeThis to STRINGS_LIKE_THIS
     *
     */
    static public String toScreamingSnakeCase(String text) {
    	toSnakeCase(text).toUpperCase()
    }

    /** */
    static public String toMarkdown(Enum en) {
        def text = en.name()
        text = text.replaceAll('_', ' ')
        text
    }    

    /** */
    static public String escapeUnderscores(String text) {
        text.replaceAll('_', '\\_')
    }    

}




