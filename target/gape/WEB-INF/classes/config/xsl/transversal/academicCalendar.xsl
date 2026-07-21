<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/academicCalendar">
        <html>
            <head>
                <title>Academic Calendar <xsl:value-of select="@initialYear"/>/<xsl:value-of select="@finalYear"/></title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f9fa; color: #212529; margin: 0; padding: 40px; }
                    .container { max-width: 950px; margin: 0 auto; background: #fff; padding: 40px; border-radius: 2px; border-top: 5px solid #004587; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }

                    h1 { color: #004587; font-size: 26px; font-weight: 700; margin-bottom: 5px; text-transform: uppercase; }
                    .sub-header { color: #6c757d; font-size: 1.1em; margin-bottom: 30px; border-bottom: 1px solid #dee2e6; padding-bottom: 10px; }

                    h2 { color: #333; font-size: 18px; margin-top: 40px; padding-bottom: 8px; border-bottom: 2px solid #004587; display: inline-block; }

                    table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 30px; }
                    th { background-color: #f8f9fa; color: #495057; padding: 12px; text-align: left; border-bottom: 2px solid #dee2e6; font-size: 0.85em; text-transform: uppercase; letter-spacing: 0.5px; }
                    td { padding: 15px 12px; border-bottom: 1px solid #e9ecef; }

                    /* Refined data colors */
                    .atividade { font-weight: 500; color: #444; width: 50%; }
                    .datas { font-family: 'Segoe UI', sans-serif; font-size: 1em; color: #004587; font-weight: 600; }

                    .badge-release { background: #f1f8e9; color: #2e7d32; padding: 3px 10px; border-radius: 3px; font-size: 0.82em; border: 1px solid #c8e6c9; display: inline-block; margin-top: 5px; }

                    tr:hover { background-color: #fcfcfc; }
                    .footer { margin-top: 60px; font-size: 0.75em; color: #adb5bd; text-align: center; border-top: 1px solid #eee; padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Academic Calendar</h1>
                    <div class="sub-header">Academic Year <xsl:value-of select="@initialYear"/> / <xsl:value-of select="@finalYear"/></div>

                    <xsl:apply-templates select="winter">
                        <xsl:with-param name="name">1st Semester (Winter)</xsl:with-param>
                    </xsl:apply-templates>

                    <xsl:apply-templates select="summer">
                        <xsl:with-param name="name">2nd Semester (Summer)</xsl:with-param>
                    </xsl:apply-templates>

                    <div class="footer">
                         <xsl:value-of select="@initialYear"/> ISEL - Instituto Superior de Engenharia de Lisboa
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="winter | summer">
        <xsl:param name="name"/>
        <h2><xsl:value-of select="$name"/></h2>
        <table>
            <thead>
                <tr>
                    <th>Academic Activity</th>
                    <th>Period</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td class="atividade">Classes and Learning Activities</td>
                    <td class="datas"><xsl:value-of select="@start"/> to <xsl:value-of select="@end"/></td>
                </tr>
                <xsl:apply-templates select="christmas | carnival | easter"/>
                <xsl:apply-templates select="normal | resit | special"/>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template match="christmas | carnival | easter">
        <tr>
            <td class="atividade">Break:
                <xsl:choose>
                    <xsl:when test="name()='christmas'">Christmas </xsl:when>
                    <xsl:when test="name()='carnival'">Carnival </xsl:when>
                    <xsl:when test="name()='easter'">Easter </xsl:when>
                </xsl:choose>
            </td>
            <td class="datas" style="color: #666;"><xsl:value-of select="@start"/> to <xsl:value-of select="@end"/></td>
        </tr>
    </xsl:template>

    <xsl:template match="normal | resit | special">
        <tr>
            <td class="atividade">Exam Period (<xsl:value-of select="name()"/>)</td>
            <td>
                <div class="datas"><xsl:value-of select="@start"/> to <xsl:value-of select="@end"/></div>
                <xsl:if test="@release">
                    <div class="badge-release">Grade release by <xsl:value-of select="@release"/></div>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>
</xsl:stylesheet>