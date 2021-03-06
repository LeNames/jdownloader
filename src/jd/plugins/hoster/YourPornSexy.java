package jd.plugins.hoster;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourporn.sexy" }, urls = { "https?://(www\\.)?yourporn\\.sexy/post/[a-fA-F0-9]{13}\\.html" })
public class YourPornSexy extends PluginForHost {
    public YourPornSexy(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        if (link.getSetLinkID() == null) {
            final String id = new Regex(link.getPluginPatternMatcher(), "/post/([a-fA-F0-9]{13})\\.html").getMatch(0);
            link.setLinkID(getHost() + "://" + id);
        }
    }

    private String dllink = null;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String title = br.getRegex("name\" content=\"(.*?)\"").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fid = new Regex(link.getLinkID(), "//([a-z0-9]+)").getMatch(0);
        String authorid = br.getRegex("data-authorid='([^']+)'").getMatch(0);
        String json = br.getRegex("data-vnfo='([^']+)'").getMatch(0);
        String vnfo = PluginJSonUtils.getJsonValue(json, fid);
        // -> vnfo: /cdn/c7/01Uyjzhs_RNzj8vcmiwHFA/1542522740/653bsds8d0gb1es6od71k4c0fdp/85mbqe8fk1m6eba8o9cd1ah94ep.mp4
        final String items[][] = new Regex(vnfo, "/cdn/([^/]+)/([^/]+)/([^/]+)/([^/]+)").getMatches();
        if (items != null && items.length > 0 && authorid != null) {
            for (final String item[] : items) {
                dllink = "https://" + item[0] + ".trafficdeposit.com/bvideo/" + item[1] + "/" + item[2] + "/" + authorid + "/" + fid + ".mp4";
            }
        }
        link.setFinalFileName(title.trim() + ".mp4");
        if (link.getVerifiedFileSize() == -1) {
            final URLConnectionAdapter con = br.cloneBrowser().openHeadConnection(dllink);
            try {
                if (con.isOK() && StringUtils.containsIgnoreCase(con.getContentType(), "video")) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                con.disconnect();
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
