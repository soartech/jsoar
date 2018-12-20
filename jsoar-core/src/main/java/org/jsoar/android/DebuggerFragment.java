package org.jsoar.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android.support.v4.app.Fragment;

import org.jsoar.R;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

public class DebuggerFragment extends Fragment
{
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    return inflater.inflate(R.layout.fragment_debugger, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    output = (TextView) getView().findViewById(R.id.logOutput);
    // So apparently you can't define the click listeners in XML for fragments
    // (This was a known problem in at least 2011)
    getView().findViewById(R.id.runButton).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        onClickRun(view);
      }
    });

    getView().findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        onClickStop(view);
      }
    });
    getView().findViewById(R.id.execButton).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        onClickExec(view);
      }
    });
    getView().findViewById(R.id.clearLogButton).setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View view)
      {
        onClickClearLog(view);
      }
    });

    AutoCompleteTextView inputTextView = (AutoCompleteTextView)getView().findViewById(R.id.inputTextView);
    inputTextView.setOnEditorActionListener(new TextView.OnEditorActionListener()
    {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
      {
        if (actionId == EditorInfo.IME_ACTION_DONE)
        {
          onClickExec(getView().findViewById(R.id.execButton));
          return true;
        }
        return false;
      }
    });
    attachAgent();
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    detachAgent();
    agent = null;
    writer = null;
    output = null;
  }

  public void onClickRun(View v)
  {
    if (!isMinimallyLoaded())
    {
      Log.w(TAG, "Run button pressed but not fully loaded!");
      return;
    }
    String typeLabel = (String) (((Spinner) getView().findViewById(R.id.runCycleTypeSelector)).getSelectedItem());
    RunType runType;
    if (typeLabel == null)
    {
      Log.w(TAG, "No cycle type selected");
      return;
    }
    else if (typeLabel.equals("Decisions"))
    {
      runType = RunType.DECISIONS;
    }
    else if (typeLabel.equals("Elaborations"))
    {
      runType = RunType.ELABORATIONS;
    }
    else if (typeLabel.equals("Forever"))
    {
      runType = RunType.FOREVER;
    }
    else if (typeLabel.equals("Phases"))
    {
      runType = RunType.PHASES;
    }
    else if (typeLabel.equals("Modifications of Output"))
    {
      runType = RunType.MODIFICATIONS_OF_OUTPUT;
    }
    else
    {
      Log.e(TAG, "Unknown or unhandled run type " + typeLabel);
      return;
    }
    // (Yes, this isn't technically necessary if FOREVER is selected, but whatever)
    int cycles;
    try
    {
      cycles = Integer.valueOf(((EditText) getView().findViewById(R.id.runCycles)).getText().toString());
      if (cycles <= 0)
      {
        Log.w(TAG, "Non positive number of cycles entered!: " + cycles);
        return;
      }
    }
    catch (NumberFormatException e)
    {
      Log.w(TAG, "Non-integer number of cycles entered!", e);
      return;
    }
    Log.i(TAG, "Run " + typeLabel + " for " + cycles);
    agent.runFor(cycles, runType);
  }

  public void onClickStop(View v)
  {
    if (!isMinimallyLoaded())
    {
      Log.w(TAG, "Stop button pressed but not fully loaded!");
      return;
    }
    Log.i(TAG, "Stopping...");
    agent.stop();
  }

  public void onClickExec(View v)
  {
    if (!isMinimallyLoaded())
    {
      Log.w(TAG, "Exec button pressed but not fully loaded!");
      return;
    }
    String command = ((AutoCompleteTextView) getView().findViewById(R.id.inputTextView)).getText().toString();
    Log.i(TAG, "Exec'ing " + command);
    addToOutput("\n> " + command);
    try
    {
      agent.getInterpreter().eval(command);
    }
    catch (SoarException e)
    {
      Log.w(TAG, "Unable to execute '" + command + "'", e);
      addToOutput("\nERROR: " + e.getLocalizedMessage());
    }
  }

  public void onClickClearLog(View v)
  {
    Log.i(TAG, "Clearing log");
    getActivity().runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        output.setText("");
      }
    });
  }

  public void setAgent(ThreadedAgent agent)
  {
    detachAgent();
    this.agent = agent;
    attachAgent();
  }

  private void detachAgent()
  {
    // No agent or no activity?  Aren't fully loaded
    if (!isMinimallyLoaded())
      return;
    setAgentControlsEnabled(false);
    if (writer != null)
    {
      agent.getPrinter().removePersistentWriter(writer);
      writer = null;
    }
  }

  private void attachAgent()
  {
    // No agent or no activity?  Aren't fully loaded
    if (!isMinimallyLoaded())
      return;
    if (writer == null)
    {
      writer = new AgentOutputWriter(getActivity());
      agent.getPrinter().addPersistentWriter(writer);
    }
    setAgentControlsEnabled(true);
  }

  private void setAgentControlsEnabled(final boolean enabled)
  {
    getActivity().runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        // I guess
        getView().findViewById(R.id.runButton).setClickable(enabled);
        getView().findViewById(R.id.runButton).setFocusable(enabled);
        getView().findViewById(R.id.stopButton).setClickable(enabled);
        getView().findViewById(R.id.stopButton).setFocusable(enabled);
        getView().findViewById(R.id.execButton).setEnabled(enabled);
      }
    });
  }

  private void addToOutput(final String message)
  {
    getActivity().runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        output.append(message);
      }
    });
  }

  private boolean isMinimallyLoaded()
  {
    return (agent != null && getActivity() != null);
  }

  private class AgentOutputWriter extends Writer
  {
    AgentOutputWriter(Activity activity)
    {
      this.activity = activity;
    }

    @Override
    public void close() throws IOException
    {
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void write(@NonNull char[] chars, int offset, int length) throws IOException
    {
      final String data = new String(Arrays.copyOfRange(chars, offset, offset + length));
      addToOutput(data);
    }

    private final Activity activity;
  }

  private static final String TAG = "DebuggerFragment";
  private TextView output;
  private Writer writer;
  private ThreadedAgent agent;
}
