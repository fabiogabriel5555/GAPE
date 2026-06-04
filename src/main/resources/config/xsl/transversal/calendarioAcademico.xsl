<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/calendarioAcademico">
        <html>
            <head>
                <title>Calendário Académico <xsl:value-of select="@anoInicial"/>/<xsl:value-of select="@anoFinal"/></title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8f9fa; color: #212529; margin: 0; padding: 40px; }
                    .container { max-width: 950px; margin: 0 auto; background: #fff; padding: 40px; border-radius: 2px; border-top: 5px solid #004587; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
                    
                    h1 { color: #004587; font-size: 26px; font-weight: 700; margin-bottom: 5px; text-transform: uppercase; }
                    .sub-header { color: #6c757d; font-size: 1.1em; margin-bottom: 30px; border-bottom: 1px solid #dee2e6; padding-bottom: 10px; }
                    
                    h2 { color: #333; font-size: 18px; margin-top: 40px; padding-bottom: 8px; border-bottom: 2px solid #004587; display: inline-block; }
                    
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 30px; }
                    th { background-color: #f8f9fa; color: #495057; padding: 12px; text-align: left; border-bottom: 2px solid #dee2e6; font-size: 0.85em; text-transform: uppercase; letter-spacing: 0.5px; }
                    td { padding: 15px 12px; border-bottom: 1px solid #e9ecef; }
                    
                    /* Cores de Dados Refinadas */
                    .atividade { font-weight: 500; color: #444; width: 50%; }
                    .datas { font-family: 'Segoe UI', sans-serif; font-size: 1em; color: #004587; font-weight: 600; }
                    
                    .badge-lancar { background: #f1f8e9; color: #2e7d32; padding: 3px 10px; border-radius: 3px; font-size: 0.82em; border: 1px solid #c8e6c9; display: inline-block; margin-top: 5px; }
                    
                    tr:hover { background-color: #fcfcfc; }
                    .footer { margin-top: 60px; font-size: 0.75em; color: #adb5bd; text-align: center; border-top: 1px solid #eee; padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Calendário Académico</h1>
                    <div class="sub-header">Ano Letivo <xsl:value-of select="@anoInicial"/> / <xsl:value-of select="@anoFinal"/></div>
                    
                    <xsl:apply-templates select="inverno">
                        <xsl:with-param name="nome">1º Semestre (Inverno)</xsl:with-param>
                    </xsl:apply-templates>

                    <xsl:apply-templates select="verao">
                        <xsl:with-param name="nome">2º Semestre (Verão)</xsl:with-param>
                    </xsl:apply-templates>
                    
                    <div class="footer">
                        © <xsl:value-of select="@anoInicial"/> ISEL - Instituto Superior de Engenharia de Lisboa
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="inverno | verao">
        <xsl:param name="nome"/>
        <h2><xsl:value-of select="$nome"/></h2>
        <table>
            <thead>
                <tr>
                    <th>Atividade Académica</th>
                    <th>Período</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td class="atividade">Aulas e Atividades Letivas</td>
                    <td class="datas"><xsl:value-of select="@inicio"/> a <xsl:value-of select="@fim"/></td>
                </tr>
                <xsl:apply-templates select="natal | carnaval | pascoa"/>
                <xsl:apply-templates select="normal | recurso | especial"/>
            </tbody>
        </table>
    </xsl:template>

    <xsl:template match="natal | carnaval | pascoa">
        <tr>
            <td class="atividade">Interrupção: 
                <xsl:choose>
                    <xsl:when test="name()='natal'">Natal 🎄</xsl:when>
                    <xsl:when test="name()='carnaval'">Carnaval 🎭</xsl:when>
                    <xsl:when test="name()='pascoa'">Páscoa 🐰</xsl:when>
                </xsl:choose>
            </td>
            <td class="datas" style="color: #666;"><xsl:value-of select="@inicio"/> a <xsl:value-of select="@fim"/></td>
        </tr>
    </xsl:template>

    <xsl:template match="normal | recurso | especial">
        <tr>
            <td class="atividade">Época de Exames (<xsl:value-of select="name()"/>)</td>
            <td>
                <div class="datas"><xsl:value-of select="@inicio"/> a <xsl:value-of select="@fim"/></div>
                <xsl:if test="@lancar">
                    <div class="badge-lancar">Lançamento de notas até <xsl:value-of select="@lancar"/></div>
                </xsl:if>
            </td>
        </tr>
    </xsl:template>
</xsl:stylesheet>