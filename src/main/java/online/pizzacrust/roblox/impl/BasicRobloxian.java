package online.pizzacrust.roblox.impl;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

import online.pizzacrust.roblox.Asset;
import online.pizzacrust.roblox.Badge;
import online.pizzacrust.roblox.ClubType;
import online.pizzacrust.roblox.Place;
import online.pizzacrust.roblox.Roblox;
import online.pizzacrust.roblox.Robloxian;
import online.pizzacrust.roblox.errors.InvalidUserException;
import online.pizzacrust.roblox.group.Group;

public class BasicRobloxian extends BasicProfile implements Robloxian {
    public BasicRobloxian(String username) throws InvalidUserException {
        super(username);
    }

    @Override
    public LightReference toReference() {
        return new BasicReference(this.getUserId(), this.getUsername());
    }

    public static class FriendsResponse {
        public static class FriendData {
            public int UserId;
            public String Username;
        }
        public FriendData[] Friends;
    }

    @Override
    public List<LightReference> getBestFriends() throws Exception {
        List<LightReference> references = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/friends/json?userId=" + getUserId() +
        "&currentPage=0&pageSize=1000&imgWidth=110&imgHeight=110&imgFormat=jpeg&friendsType" +
                "=BestFriends";
        FriendsResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), FriendsResponse.class);
        for (FriendsResponse.FriendData friend : response.Friends) {
            references.add(new BasicReference(friend.UserId, friend.Username));
        }
        return references;
    }

    public static class BadgeResponse {
        public static class BadgeData {
            public String Name;
        }
        public BadgeData[] RobloxBadges;
    }

    @Override
    public List<String> getRobloxBadges() throws Exception{
        List<String> badges = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/badges/roblox?userId=261&imgWidth=110&imgHeight=110&imgFormat=png";
        BadgeResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), BadgeResponse.class);
        for (BadgeResponse.BadgeData robloxBadge : response.RobloxBadges) {
            badges.add(robloxBadge.Name);
        }
        return badges;
    }

    @Override
    public List<String> getPastUsernames() throws Exception {
        List<String> names = new ArrayList<>();
        Document document = Jsoup.connect(getProfileUrl()).ignoreContentType(true).get();
        Element rootNode = document.getElementsByClass("profile-name-history").first();
        Element pastNamesElement = rootNode.getElementsByClass("tooltip-pastnames").first();
        String pastNamesUnparsed = pastNamesElement.attr("title");
        String[] splitted = pastNamesUnparsed.split(",");
        for (String s : splitted) {
            names.add(s.trim());
        }
        return names;
    }

    @Override
    public boolean isInGroup(Group groupId) throws Exception{
        String url = "https://www.roblox.com/Game/LuaWebService/HandleSocialRequest" +
                ".ashx?method=IsInGroup&playerid=" + this.getUserId() + "&groupid=" + groupId.getId();
        return Boolean.parseBoolean(Jsoup.connect(url).ignoreContentType(true).get().body
                ().text());
    }

    @Override
    public String getProfileUrl() {
        return "https://www.roblox.com/users/" + this.getUserId() + "/profile";
    }

    public static class GroupsResponse {
        public static class GroupData {
            public int Id;
        }
        public GroupData[] Groups;
    }

    @Override
    public Group[] getGroups() throws Exception {
        String url = "https://www.roblox.com/users/profile/playergroups-json?userId=" + this
                .getUserId();
        GroupsResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), GroupsResponse.class);
        List<Group> groups = new ArrayList<>();
        for (GroupsResponse.GroupData group : response.Groups) {
            groups.add(Roblox.get(group.Id));
        }
        return groups.toArray(new Group[groups.size()]);
    }

    public static class BadgesResponse {
        public static class BadgesData {
            public String nextPageCursor;
            public static class PlayerBadgeData {
                public static class ItemData {
                    public int AssetId;
                    public String Name;
                }
                public ItemData Item;
            }
            public PlayerBadgeData[] Items;
        }
        public BadgesData Data;
    }

    private List<Badge> recursiveBadgeRetrieve(BadgesResponse response) throws Exception {
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=21&cursor=" + response
                .Data.nextPageCursor + "&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        BadgesResponse response1 = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), BadgesResponse.class);
        List<Badge> badges = new ArrayList<>();
        for (BadgesResponse.BadgesData.PlayerBadgeData item : response1.Data.Items) {
            badges.add(new Badge(item.Item.AssetId, item.Item.Name));
        }
        if (response1.Data.nextPageCursor != null) {
            badges.addAll(recursiveBadgeRetrieve(response1));
        }
        return badges;
    }

    public static class AssetDataData {
        public static class Data {
            public String nextPageCursor;
        }
        public Data Data;
    }

    private List<String> getAssetRecursively(int assetId, String cursor) throws Exception {
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=" + assetId + "&cursor=" + cursor +
                "&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        String string = Jsoup.connect(url).ignoreContentType(true)
                .get().body().text();
        AssetDataData response1 = new Gson().fromJson(string, AssetDataData.class);
        List<String> pages = new ArrayList<>();
        pages.add(string);
        if (response1.Data.nextPageCursor != null) {
            pages.addAll(getAssetRecursively(assetId, response1.Data.nextPageCursor));
        }
        return pages;
    }

    private List<String> getAssetInventory(int assetId) throws Exception {
        List<String> strings = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=" + assetId + "&cursor="  +
                "&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        String string = Jsoup.connect(url).ignoreContentType(true)
                .get().body().text();
        AssetDataData response1 = new Gson().fromJson(string, AssetDataData.class);
        strings.add(string);
        if (response1.Data.nextPageCursor != null) {
            strings.addAll(getAssetRecursively(assetId, response1.Data.nextPageCursor));
        }
        return strings;
    }

    @Override
    public Badge[] getBadges() throws Exception {
        List<Badge> badges = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=21&cursor=&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        BadgesResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), BadgesResponse.class);
        for (BadgesResponse.BadgesData.PlayerBadgeData item : response.Data.Items) {
            badges.add(new Badge(item.Item.AssetId, item.Item.Name));
        }
        // next page
        if (response.Data.nextPageCursor != null) {
            badges.addAll(recursiveBadgeRetrieve(response));
        }
        return badges.toArray(new Badge[badges.size()]);
    }

    public static class PlacesResponse {
        public static class GameData {
            public String Description;
            public String Name;
            public int Plays;
            public int PlaceID;
        }
        public GameData[] Games;
    }

    @Override
    public Place[] getPlaces() throws Exception {
        PlacesResponse response = new Gson().fromJson(Jsoup.connect("https://www.roblox" +
                ".com/users/profile/playergames-json?userId=" + getUserId()).ignoreContentType
                (true).get().body().text(), PlacesResponse.class);
        Place[] places = new Place[response.Games.length];
        for (int i = 0; i < response.Games.length; i++) {
            PlacesResponse.GameData data = response.Games[i];
            places[i] = new BasicPlace(data.PlaceID, data.Plays, data.Name, data.Description);
        }
        return places;
    }

    @Override
    public Asset[] getShirts() throws Exception {
        return BasicAsset.getAssetsToArray(getAssetInventory(11));
    }

    @Override
    public Asset[] getPants() throws Exception {
        return BasicAsset.getAssetsToArray(getAssetInventory(12));
    }

    @Override
    public Asset[] getTshirts() throws Exception {
        return BasicAsset.getAssetsToArray(getAssetInventory(2));
    }

    @Override
    public int getJoinTimeInDays() throws Exception {
        return 0;
    }

    @Override
    public int getForumPostAmount() throws Exception {
        return 0;
    }

    @Override
    public String getStatus() throws Exception {
        return null;
    }

    @Override
    public String getDescription() throws Exception {
        Document document = Jsoup.connect(getProfileUrl()).ignoreContentType(true).get();
        return document.getElementsByClass("profile-about-content-text").first().text();
    }

    @Override
    public ClubType getClub() throws Exception {
        return null;
    }

    @Override
    public int getAmountOfFollowers() throws Exception {
        return 0;
    }

    @Override
    public int getAmountFollowing() throws Exception {
        return 0;
    }

    public static void main(String... args) throws Exception {
        BasicRobloxian robloxian = new BasicRobloxian("TGSCommander");
        System.out.println(robloxian.getDescription());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicRobloxian)) {
            return false;
        }
        BasicRobloxian robloxian = (BasicRobloxian) obj;
        return robloxian.getUserId() == this.getUserId();
    }

}