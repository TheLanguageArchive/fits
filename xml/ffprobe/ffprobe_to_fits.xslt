<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://hul.harvard.edu/ois/xml/ns/fits/fits_output">
    <xsl:template match="/ffprobe">
        <xsl:variable name="video" select="streams/stream[@codec_type='video'][not(disposition/@attached_pic='1')]"/>
        <xsl:variable name="audio" select="streams/stream[@codec_type='audio']"/>
        <fits>
            <xsl:if test="$video">
                <metadata>
                    <video>
                        <!-- FITS uses the overall bitrate at this level. -->
                        <xsl:if test="format/@bit_rate">
                            <bitRate><xsl:value-of select="format/@bit_rate"/></bitRate>
                        </xsl:if>
                        <xsl:apply-templates select="$video | $audio"/>
                    </video>
                </metadata>
            </xsl:if>
        </fits>
    </xsl:template>

    <xsl:template match="stream">
        <track type="{@codec_type}" id="{@index + 1}">
            <xsl:if test="@codec_type='video' and @codec_tag_string">
                <videoDataEncoding><xsl:value-of select="@codec_tag_string"/></videoDataEncoding>
            </xsl:if>
            <xsl:if test="@codec_name">
                <codecFamily>
                    <xsl:value-of select="if (@codec_name='h264') then 'H.264'
                        else if (@codec_name='aac') then 'AAC' else @codec_name"/>
                </codecFamily>
            </xsl:if>
            <xsl:if test="@channels">
                <channels><xsl:value-of select="@channels"/></channels>
            </xsl:if>
        </track>
    </xsl:template>
</xsl:stylesheet>
