/*
ChatCreationFragment.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package org.linphone.chat;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.linphone.LinphoneManager;
import org.linphone.contacts.ContactAddress;
import org.linphone.contacts.SearchContactsListAdapter;
import org.linphone.core.Address;
import org.linphone.core.ChatRoom;
import org.linphone.core.ChatRoomListenerStub;
import org.linphone.core.Core;
import org.linphone.mediastream.Log;
import org.linphone.ui.ContactSelectView;
import org.linphone.contacts.ContactsUpdatedListener;
import org.linphone.activities.LinphoneActivity;
import org.linphone.R;

import java.util.ArrayList;
import java.util.List;

public class ChatCreationFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener, ContactsUpdatedListener {
	private LayoutInflater mInflater;
	private ListView mContactsList;
	private LinearLayout mContactsSelectedLayout;
	private HorizontalScrollView mContactsSelectLayout;
	private ArrayList<ContactAddress> mContactsSelected;
	private ImageView mAllContactsButton, mLinphoneContactsButton, mClearSearchFieldButton, mBackButton, mNextButton;
	private boolean mOnlyDisplayLinphoneContacts;
	private View mAllContactsSelected, mLinphoneContactsSelected;
	private RelativeLayout mSearchLayout, mWaitLayout;
	private EditText mSearchField;
	private ProgressBar mContactsFetchInProgress;
	private SearchContactsListAdapter mSearchAdapter;
	private String mChatRoomSubject, mChatRoomAddress;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mInflater = inflater;
		View view = inflater.inflate(R.layout.chat_create, container, false);

		mContactsSelected = new ArrayList<>();
		mChatRoomSubject = null;
		mChatRoomAddress = null;
		if (getArguments() != null) {
			if (getArguments().getSerializable("selectedContacts") != null) {
				mContactsSelected = (ArrayList<ContactAddress>) getArguments().getSerializable("selectedContacts");
			}
			mChatRoomSubject = getArguments().getString("subject");
			mChatRoomAddress = getArguments().getString("groupChatRoomAddress");
		}

		mWaitLayout = view.findViewById(R.id.waitScreen);
		mWaitLayout.setVisibility(View.GONE);

		mContactsList = view.findViewById(R.id.contactsList);
		mContactsSelectedLayout = view.findViewById(R.id.contactsSelected);
		mContactsSelectLayout = view.findViewById(R.id.layoutContactsSelected);

		mAllContactsButton = view.findViewById(R.id.all_contacts);
		mAllContactsButton.setOnClickListener(this);

		mLinphoneContactsButton = view.findViewById(R.id.linphone_contacts);
		mLinphoneContactsButton.setOnClickListener(this);

		mAllContactsSelected = view.findViewById(R.id.all_contacts_select);
		mLinphoneContactsSelected = view.findViewById(R.id.linphone_contacts_select);

		mBackButton = view.findViewById(R.id.back);
		mBackButton.setOnClickListener(this);

		mNextButton = view.findViewById(R.id.next);
		mNextButton.setOnClickListener(this);
		mNextButton.setEnabled(false);
		mSearchLayout = view.findViewById(R.id.layoutSearchField);

		mClearSearchFieldButton = view.findViewById(R.id.clearSearchField);
		mClearSearchFieldButton.setOnClickListener(this);

		mContactsFetchInProgress = view.findViewById(R.id.contactsFetchInProgress);
		mContactsFetchInProgress.setVisibility(View.VISIBLE);

		mSearchAdapter = new SearchContactsListAdapter(null, mInflater, mContactsFetchInProgress);

		mSearchField = view.findViewById(R.id.searchField);
		mSearchField.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				mSearchAdapter.searchContacts(mSearchField.getText().toString(), mContactsList);
			}
		});

		mContactsList.setAdapter(mSearchAdapter);
		mContactsList.setOnItemClickListener(this);
		if (savedInstanceState != null && savedInstanceState.getStringArrayList("mContactsSelected") != null) {
			mContactsSelectedLayout.removeAllViews();
			// We need to get all contacts not only sip
			for (String uri : savedInstanceState.getStringArrayList("mContactsSelected")) {
				for (ContactAddress ca : mSearchAdapter.getContactsList()) {
					if (ca.getAddressAsDisplayableString().compareTo(uri) == 0) {
						ca.setView(null);
						addSelectedContactAddress(ca);
						break;
					}
				}
			}
			updateList();
			updateListSelected();
		}

		mOnlyDisplayLinphoneContacts = true;
		if (savedInstanceState != null ) {
			mOnlyDisplayLinphoneContacts = savedInstanceState.getBoolean("onlySipContact", true);
		}
		mSearchAdapter.setOnlySipContact(mOnlyDisplayLinphoneContacts);
		updateList();

		displayChatCreation();

		return view;
	}

	private void displayChatCreation() {
		mNextButton.setVisibility(View.VISIBLE);
		mNextButton.setEnabled(mContactsSelected.size() > 0);

		mContactsList.setVisibility(View.VISIBLE);
		mSearchLayout.setVisibility(View.VISIBLE);
		mAllContactsButton.setVisibility(View.VISIBLE);
		mLinphoneContactsButton.setVisibility(View.VISIBLE);
		if (mOnlyDisplayLinphoneContacts) {
			mAllContactsSelected.setVisibility(View.INVISIBLE);
			mLinphoneContactsSelected.setVisibility(View.VISIBLE);
		} else {
			mAllContactsSelected.setVisibility(View.VISIBLE);
			mLinphoneContactsSelected.setVisibility(View.INVISIBLE);
		}

		mAllContactsButton.setEnabled(mOnlyDisplayLinphoneContacts);
		mLinphoneContactsButton.setEnabled(!mAllContactsButton.isEnabled());

		mContactsSelectedLayout.removeAllViews();
		if (mContactsSelected.size() > 0) {
			mSearchAdapter.setContactsSelectedList(mContactsSelected);
			for (ContactAddress ca : mContactsSelected) {
				addSelectedContactAddress(ca);
			}
		}
	}

	private void updateList() {
		mSearchAdapter.searchContacts(mSearchField.getText().toString(), mContactsList);
		mSearchAdapter.notifyDataSetChanged();
	}

	private void updateListSelected() {
		if (mContactsSelected.size() > 0) {
			mContactsSelectLayout.invalidate();
			mNextButton.setEnabled(true);
		} else {
			mNextButton.setEnabled(false);
		}
	}

	private int getIndexOfCa(ContactAddress ca, List<ContactAddress> caList) {
		for (int i = 0 ; i < caList.size() ; i++) {
			if (caList.get(i).getAddressAsDisplayableString().compareTo(ca.getAddressAsDisplayableString()) == 0)
				return i;
		}
		return -1;
	}

	private void addSelectedContactAddress(ContactAddress ca) {
		View viewContact = LayoutInflater.from(LinphoneActivity.instance()).inflate(R.layout.contact_selected, null);
		if (ca.getContact() != null) {
			((TextView) viewContact.findViewById(R.id.sipUri)).setText(ca.getContact().getFullName());
		} else {
			((TextView) viewContact.findViewById(R.id.sipUri)).setText(ca.getAddressAsDisplayableString());
		}
		View removeContact = viewContact.findViewById(R.id.contactChatDelete);
		removeContact.setTag(ca);
		removeContact.setOnClickListener(this);
		viewContact.setOnClickListener(this);
		ca.setView(viewContact);
		mContactsSelectedLayout.addView(viewContact);
		mContactsSelectedLayout.invalidate();
	}

	private void updateContactsClick(ContactAddress ca, List<ContactAddress> caSelectedList) {
		ca.setSelect((getIndexOfCa(ca, caSelectedList) == -1));
		if (ca.isSelect()) {
			ContactSelectView csv = new ContactSelectView(LinphoneActivity.instance());
			csv.setListener(this);
			csv.setContactName(ca);
			mContactsSelected.add(ca);
			addSelectedContactAddress(ca);
		} else {
			mContactsSelected.remove(getIndexOfCa(ca, mContactsSelected));
			mContactsSelectedLayout.removeAllViews();
			for (ContactAddress contactAddress : mContactsSelected) {
				if (contactAddress.getView() != null)
					mContactsSelectedLayout.addView(contactAddress.getView());
			}
		}
		mSearchAdapter.setContactsSelectedList(mContactsSelected);
		mContactsSelectedLayout.invalidate();

	}

	private void removeContactFromSelection(ContactAddress ca) {
		updateContactsClick(ca, mSearchAdapter.getContactsSelectedList());
		mSearchAdapter.notifyDataSetInvalidated();
		updateListSelected();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (mContactsSelected != null && mContactsSelected.size() > 0) {
			ArrayList<String> listUri = new ArrayList<String>();
			for (ContactAddress ca : mContactsSelected) {
				listUri.add(ca.getAddressAsDisplayableString());
			}
			outState.putStringArrayList("mContactsSelected", listUri);
		}

		outState.putBoolean("onlySipContact", mOnlyDisplayLinphoneContacts);

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.all_contacts) {
			mSearchAdapter.setOnlySipContact(mOnlyDisplayLinphoneContacts = false);
			mAllContactsSelected.setVisibility(View.VISIBLE);
			mAllContactsButton.setEnabled(false);
			mLinphoneContactsButton.setEnabled(true);
			mLinphoneContactsSelected.setVisibility(View.INVISIBLE);
			updateList();
		} else if (id == R.id.linphone_contacts) {
			mSearchAdapter.setOnlySipContact(true);
			mLinphoneContactsSelected.setVisibility(View.VISIBLE);
			mLinphoneContactsButton.setEnabled(false);
			mAllContactsButton.setEnabled(mOnlyDisplayLinphoneContacts = true);
			mAllContactsSelected.setVisibility(View.INVISIBLE);
			updateList();
		} else if (id == R.id.back) {
			mContactsSelectedLayout.removeAllViews();
			LinphoneActivity.instance().popBackStack();
		} else if (id == R.id.next) {
			if (mChatRoomAddress == null && mChatRoomSubject == null) {
				if (mContactsSelected.size() == 1) {
					mContactsSelectedLayout.removeAllViews();
					mWaitLayout.setVisibility(View.VISIBLE);
					Core lc = LinphoneManager.getLc();
					Address participant = mContactsSelected.get(0).getAddress();
					ChatRoom chatRoom = lc.findOneToOneChatRoom(lc.getDefaultProxyConfig().getContact(), participant);
					if (chatRoom == null) {
						chatRoom = lc.createClientGroupChatRoom(getString(R.string.dummy_group_chat_subject));
						chatRoom.setListener(new ChatRoomListenerStub() {
							@Override
							public void onStateChanged(ChatRoom cr, ChatRoom.State newState) {
								if (newState == ChatRoom.State.Created) {
									cr.setListener(null);
									mWaitLayout.setVisibility(View.GONE);
									LinphoneActivity.instance().goToChat(cr.getPeerAddress().asStringUriOnly());
								} else if (newState == ChatRoom.State.CreationFailed) {
									cr.setListener(null);
									mWaitLayout.setVisibility(View.GONE);
									LinphoneActivity.instance().displayChatRoomError();
									Log.e("Group chat room for address " + cr.getPeerAddress() + " has failed !");
								}
							}
						});

						chatRoom.addParticipant(participant);
					} else {
						LinphoneActivity.instance().goToChat(chatRoom.getPeerAddress().asStringUriOnly());
					}
				} else {
					mContactsSelectedLayout.removeAllViews();
					LinphoneActivity.instance().goToChatGroupInfos(null, mContactsSelected, null, true, false);
				}
			} else {
				LinphoneActivity.instance().goToChatGroupInfos(mChatRoomAddress, mContactsSelected, mChatRoomSubject, true, true);
			}
		} else if (id == R.id.clearSearchField) {
			mSearchField.setText("");
			mSearchAdapter.searchContacts("", mContactsList);
		} else if (id == R.id.contactChatDelete) {
			ContactAddress ca = (ContactAddress) view.getTag();
			removeContactFromSelection(ca);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		ContactAddress ca = mSearchAdapter.getContacts().get(i);
		removeContactFromSelection(ca);
	}

	@Override
	public void onContactsUpdated() {
		mSearchAdapter.searchContacts(mSearchField.getText().toString(), mContactsList);
	}
}
